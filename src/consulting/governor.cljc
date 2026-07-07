(ns consulting.governor
  "Consulting Engagement Governor -- the independent compliance layer
  that earns the Consultant-LLM the right to commit. The LLM has no
  notion of professional-standards law, whether an engagement's own
  proposed recommendation topics actually stay within its own
  contracted scope, whether a conflict of interest against the
  engagement has actually stayed unresolved, or when an act stops
  being a draft and becomes a real-world deliverable issued to a
  client, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD -- the management-consultancy analog
  of `cloud-itonami-isic-6512`'s CasualtyGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated professional-standards spec-basis, incomplete
  evidence, a deliverable that exceeds the contracted scope, an
  unresolved conflict of interest, or a double issuance). The
  confidence/actuation gate is SOFT: it asks a human to look (low
  confidence / actuation), and the human may approve -- but see
  `consulting.phase`: for `:stake :actuation/issue-deliverable` (a
  real client-facing act) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the finding/research proposal
                                       cite an OFFICIAL professional-
                                       standards source (`consulting.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/issue-
                                       deliverable`, has the engagement
                                       actually had findings researched
                                       with a full scope-of-engagement-
                                       document/client-data-access-
                                       authorization-record/
                                       methodology-citation-record/
                                       conflict-of-interest-disclosure-
                                       record evidence checklist on
                                       file?
    3. Engagement scope exceeded    -- for `:actuation/issue-
                                       deliverable`, INDEPENDENTLY
                                       recompute whether the
                                       engagement's own recommendation
                                       topics stay within its own
                                       contracted scope items
                                       (`consulting.registry/
                                       engagement-scope-exceeded?`) --
                                       needs no proposal inspection or
                                       stored-verdict lookup at all.
                                       The FOURTH instance of this
                                       fleet's set-containment/subset
                                       check family (`registrar.
                                       governor/prerequisites-not-
                                       satisfied-violations`/
                                       `casework.governor/eligibility-
                                       criteria-unsatisfied-violations`/
                                       `secondary.governor/graduation-
                                       requirements-unsatisfied-
                                       violations` established the
                                       first three).
    4. Conflict of interest unresolved -- reported by THIS proposal
                                       itself (a `:conflict/screen`
                                       that just found one), or already
                                       on file for the engagement
                                       (`:conflict/screen`/`:actuation/
                                       issue-deliverable`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/...
                                       (twenty-eight prior siblings,
                                       most recently `recovery.
                                       governor/contamination-flag-
                                       unresolved-violations`)...
                                       established -- the TWENTY-NINTH
                                       distinct application of this
                                       exact discipline overall. The
                                       specific conflict-of-interest
                                       CONCEPT itself is a REUSE, not
                                       new -- `adjustment.governor`
                                       (1st), `intermediation.governor`
                                       (2nd) and `brokerage.governor`
                                       (3rd) already ground it in
                                       insurance-adjustment/insurance-
                                       intermediation/securities-
                                       brokerage contexts (confirmed via
                                       grep before writing this
                                       docstring, avoiding the false-
                                       precedent-claim risk `leasing`'s
                                       ADR-0001 documents); this is the
                                       FOURTH grounding of that specific
                                       concept, and the FIRST outside a
                                       financial-services context (a
                                       general professional-services/
                                       management-consulting
                                       engagement). Exercised in
                                       tests/demo via `:conflict/
                                       screen` DIRECTLY, not via the
                                       actuation op against an
                                       unscreened engagement -- see
                                       this ns's own test suite.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/issue-
                                       deliverable` (a REAL client-
                                       facing act) -> escalate.

  One more guard, double-issuance prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-issued-violations` refuses to issue a
  deliverable for the SAME engagement twice, off a dedicated
  `:deliverable-issued?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [consulting.facts :as facts]
            [consulting.registry :as registry]
            [consulting.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Issuing a real recommendation/deliverable to a client is the ONE
  real-world actuation event this actor performs -- a single-member
  set, matching `leasing`'s/`underwriting`'s/`testlab`'s/`clinic`'s/
  `veterinary`'s/`funeral`'s/`parksafety`'s/`salon`'s/
  `entertainment`'s/`facility`'s single-actuation shape."
  #{:actuation/issue-deliverable})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:finding/research` (or `:actuation/issue-deliverable`) proposal
  with no spec-basis citation is a HARD violation -- never invent a
  jurisdiction's professional-standards requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:finding/research :actuation/issue-deliverable} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は専門職基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/issue-deliverable`, the jurisdiction's required
  scope-of-engagement-document/client-data-access-authorization-
  record/methodology-citation-record/conflict-of-interest-disclosure-
  record evidence must actually be satisfied -- do not trust the
  advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-deliverable)
    (let [e (store/engagement st subject)
          finding (store/finding-of st subject)]
      (when-not (and finding
                     (facts/required-evidence-satisfied?
                      (:jurisdiction e) (:checklist finding)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(契約範囲確認書/クライアントデータアクセス許可記録/方法論引用記録/利益相反開示記録等)が充足していない状態での提案"}]))))

(defn- engagement-scope-exceeded-violations
  "For `:actuation/issue-deliverable`, INDEPENDENTLY recompute whether
  the engagement's own recommendation topics stay within its own
  contracted scope items via `consulting.registry/engagement-scope-
  exceeded?` -- needs no proposal inspection or stored-verdict lookup
  at all, since its inputs are permanent ground-truth fields already
  on the engagement."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-deliverable)
    (let [e (store/engagement st subject)]
      (when (registry/engagement-scope-exceeded? e)
        [{:rule :engagement-scope-exceeded
          :detail (str subject " の提案トピック(" (:recommendation-topics e)
                      ")が契約範囲(" (:contracted-scope-items e) ")を超過")}]))))

(defn- conflict-of-interest-unresolved-violations
  "An unresolved conflict of interest -- reported by THIS proposal
  (e.g. a `:conflict/screen` that itself just found one), or already
  on file in the store for the engagement (`:conflict/screen`/
  `:actuation/issue-deliverable`) -- is a HARD, un-overridable hold.
  Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        engagement-id (when (contains? #{:conflict/screen :actuation/issue-deliverable} op) subject)
        hit-on-file? (and engagement-id (= :unresolved (:verdict (store/conflict-screen-of st engagement-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :conflict-of-interest-unresolved
        :detail "未解決の利益相反がある状態での成果物発行提案は進められない"}])))

(defn- already-issued-violations
  "For `:actuation/issue-deliverable`, refuses to issue a deliverable
  for the SAME engagement twice, off a dedicated `:deliverable-
  issued?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-deliverable)
    (when (store/engagement-already-issued? st subject)
      [{:rule :already-issued
        :detail (str subject " は既に成果物発行済み")}])))

(defn check
  "Censors a Consultant-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (engagement-scope-exceeded-violations request st)
                           (conflict-of-interest-unresolved-violations request proposal st)
                           (already-issued-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
