(ns consulting.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout ledger): this repo previously had NO demo page and no
  generator at all. This namespace drives the REAL actor stack
  (`consulting.operation` -> `consulting.governor` -> `consulting.store`)
  through a scenario adapted from this repo's own `consulting.sim` demo
  driver (`clojure -M:dev:run`, confirmed by actually running it before
  this file was written -- unlike `cloud-itonami-isic-851`'s
  `schoolops.sim`, this repo's own sim driver uses engagement ids that DO
  match `consulting.store/demo-data`'s seeded engagements exactly, and
  every disposition it produces (auto-commit / escalate+approve / HARD
  hold, and the exact `:rule` on each hold) matches
  `consulting.governor`'s own documented checks precisely -- verified by
  running `clojure -M:dev:run` and diffing its printed audit ledger
  against the governor's docstring before reuse, so it was safe to reuse
  rather than author from scratch), trimmed to a representative subset
  (the one phase-3 auto-commit this domain has, the full
  research/screen/issue-deliverable escalation lifecycle for one clean
  engagement, and four distinct HARD-hold reasons that never reach a
  human) and rendered deterministically -- no invented numbers, no
  timestamps in the page content, byte-identical across reruns against
  the same seed (verified by diffing two consecutive runs before
  shipping).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [consulting.store :as store]
            [consulting.operation :as op]
            [langgraph.graph :as g]))

;; ----------------------------- harness (unchanged across every repo
;; in this cluster -- do not rewrite, only copy) -----------------------

(def ^:private operator
  {:actor-id "op-1" :actor-role :consultant :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach, using ONLY real engagement ids from
  `consulting.store/demo-data`:

  engagement-1 (Sakura Cooperative, JPN, clean, contracted-scope
  #{:strategy :operations}, recommendation-topics #{:strategy}, no
  conflict of interest) walks the full clean lifecycle: an
  `:engagement/intake` directory-normalization patch is a phase-3,
  no-capital-risk auto-commit (governor clean, high confidence,
  `:engagement/intake` is the ONLY op in phase 3's `:auto` set);
  `:finding/research` (JPN has a real spec-basis in `consulting.facts`)
  and `:conflict/screen` (clean) each ALWAYS escalate (neither op is
  ever auto-eligible, at any phase) and are approved by a human
  consultant; `:actuation/issue-deliverable` -- the ONE real-world
  actuation event this actor performs (a real recommendation/deliverable
  issued to a client) -- ALSO ALWAYS escalates (the governor's own
  `high-stakes` gate AND the phase table agree, independently, that
  actuation is never auto, at any phase) and is approved, producing one
  draft deliverable-issuance record (`JPN-DLV-000000`).

  Then four DISTINCT HARD-hold reasons, none of which ever reach a
  human (a human approver cannot override a HARD violation):
    - engagement-2 (Atlantis Collective, jurisdiction ATL, not in
      `consulting.facts/catalog`): `:finding/research` HARD-holds on
      `:no-spec-basis` -- the advisor may not invent a jurisdiction's
      professional-standards requirements.
    - engagement-3 (鈴木商店, JPN, recommendation-topics
      #{:strategy :governance} but contracted-scope only
      #{:strategy :operations}): assessed first (`:finding/research`
      clean escalate+approve, so evidence is on file and the hold below
      is isolated to the scope check alone), then
      `:actuation/issue-deliverable` HARD-holds on
      `:engagement-scope-exceeded` -- the governor independently
      recomputes whether the engagement's own proposed recommendation
      topics stay within its own contracted scope items, never trusting
      the advisor's proposal.
    - engagement-4 (田中商事, `:conflict-of-interest-unresolved? true` in
      the seed data): `:conflict/screen` HARD-holds on
      `:conflict-of-interest-unresolved` -- an unresolved conflict of
      interest blocks progress, un-overridably, even though the
      screening op itself is the one that (re)discovers it.
    - engagement-1 AGAIN (`:actuation/issue-deliverable` a second time,
      after the first issuance already committed above):
      HARD-holds on `:already-issued` -- the governor refuses to issue a
      deliverable for the same engagement twice.

  Returns the resulting store -- every field `render` below reads is
  real governor/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]

    ;; engagement-1: clean directory-normalization patch -- phase-3
    ;; auto-commit, no capital risk yet.
    (exec! actor "e1-intake" {:op :engagement/intake :subject "engagement-1"
                              :patch {:id "engagement-1" :client-name "Sakura Cooperative"}})

    ;; engagement-1: professional-standards evidence-checklist research
    ;; (JPN has a real spec-basis) -- ALWAYS escalates, approved by a
    ;; human consultant.
    (exec! actor "e1-research" {:op :finding/research :subject "engagement-1"})
    (approve! actor "e1-research")

    ;; engagement-1: conflict-of-interest screening, clean -- ALWAYS
    ;; escalates, approved by a human consultant.
    (exec! actor "e1-screen" {:op :conflict/screen :subject "engagement-1"})
    (approve! actor "e1-screen")

    ;; engagement-1: REAL deliverable issuance (actuation/issue-
    ;; deliverable, a real recommendation issued to a client) -- ALWAYS
    ;; escalates regardless of phase or confidence, approved by a human
    ;; consultant.
    (exec! actor "e1-issue" {:op :actuation/issue-deliverable :subject "engagement-1"})
    (approve! actor "e1-issue")

    ;; engagement-2 (ATL): no official spec-basis in consulting.facts ->
    ;; HARD hold on :no-spec-basis, never reaches a human.
    (exec! actor "e2-research" {:op :finding/research :subject "engagement-2" :no-spec? true})

    ;; engagement-3: research JPN first (clean escalate+approve) so
    ;; evidence is on file and the scope-exceeded hold below is
    ;; isolated.
    (exec! actor "e3-research" {:op :finding/research :subject "engagement-3"})
    (approve! actor "e3-research")

    ;; engagement-3: recommendation-topics #{:strategy :governance}
    ;; exceeds contracted-scope-items #{:strategy :operations} -> HARD
    ;; hold on :engagement-scope-exceeded, never reaches a human.
    (exec! actor "e3-issue" {:op :actuation/issue-deliverable :subject "engagement-3"})

    ;; engagement-4: seeded with an unresolved conflict of interest ->
    ;; HARD hold on :conflict-of-interest-unresolved, never reaches a
    ;; human.
    (exec! actor "e4-screen" {:op :conflict/screen :subject "engagement-4"})

    ;; engagement-1 AGAIN: double deliverable-issuance -> HARD hold on
    ;; :already-issued, never reaches a human.
    (exec! actor "e1-issue-again" {:op :actuation/issue-deliverable :subject "engagement-1"})

    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for [ledger subject-id]
  (last (filter #(= (:subject %) subject-id) ledger)))

(defn- status-cell [ledger subject-id]
  (let [f (last-fact-for ledger subject-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (-> f :violations first :rule)]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- topics-str [ks]
  (str/join ", " (map name (sort ks))))

(defn- engagement-row [ledger {:keys [id client-name jurisdiction contracted-scope-items
                                       recommendation-topics conflict-of-interest-unresolved?
                                       deliverable-issued? issuance-number]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc client-name) (esc jurisdiction)
          (esc (topics-str contracted-scope-items)) (esc (topics-str recommendation-topics))
          (if conflict-of-interest-unresolved? "<span class=\"critical\">unresolved</span>" "<span class=\"ok\">clear</span>")
          (if deliverable-issued? (str "issued &middot; " (esc issuance-number)) "not issued")
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc subject)
          (esc (or (some->> basis (map #(if (keyword? %) (name %) %)) (str/join ", "))
                    (some-> disposition name) ""))))

(defn- record-row [{:strs [record_id engagement_id jurisdiction kind immutable]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc record_id) (esc engagement_id) (esc jurisdiction)
          (if immutable "<span class=\"ok\">immutable draft</span>" (esc kind))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract
  ;; (`consulting.governor`/`consulting.phase`) -- documentation of
  ;; fixed behavior, not runtime telemetry, so it is legitimately hand-
  ;; described rather than derived from a live run.
  ["        <tr><td><code>:engagement/intake</code></td><td><span class=\"ok\">phase-3 auto-commit when clean, no capital risk yet -- the ONLY auto-eligible op in this domain</span></td></tr>"
   "        <tr><td><code>:finding/research</code></td><td><span class=\"warn\">ALWAYS human approval &middot; spec-basis independently checked against <code>consulting.facts</code>, never fabricated</span></td></tr>"
   "        <tr><td><code>:conflict/screen</code></td><td><span class=\"warn\">ALWAYS human approval when clean &middot; an unresolved conflict of interest is a HARD, un-overridable hold instead</span></td></tr>"
   "        <tr><td><code>:actuation/issue-deliverable</code></td><td><span class=\"warn\">ALWAYS human approval &middot; a real recommendation/deliverable issued to a client &middot; evidence-completeness + engagement-scope-exceeded + double-issuance guards enforced, never auto at any phase</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        engagements (store/all-engagements db)
        engagement-rows (str/join "\n" (map (partial engagement-row ledger) engagements))
        ledger-rows (str/join "\n" (map ledger-row ledger))
        deliverable-rows (str/join "\n" (map record-row (store/deliverable-history db)))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-7020 &middot; management consultancy activities</title><style>\n"
     "table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
     ".ok { color: #137a3f; }\n"
     "body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }\n"
     "header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }\n"
     "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }\n"
     "h2 { margin-top: 0; font-size: 15px; }\n"
     ".warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }\n"
     "main { max-width: 980px; margin: 24px auto; padding: 0 20px; }\n"
     "header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }\n"
     ".muted { color: #888; font-size: 13px; }\n"
     ".critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n"
     ".card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n"
     ".err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }\n"
     "th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }\n"
     "header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }\n"
     "code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }\n"
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Management consultancy activities (ISIC 7020) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · deliverable issuance always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Engagements</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>consulting.store</code> via <code>consulting.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Engagement</th><th>Client</th><th>Jurisdiction</th><th>Contracted scope</th><th>Recommendation topics</th><th>Conflict of interest</th><th>Deliverable</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     engagement-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Draft deliverable-issuance records</h2>\n"
     "    <p class=\"muted\">Unsigned drafts only — the consultancy's own act of issuing the deliverable is outside this actor's authority (see README <code>Actuation</code>).</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record id</th><th>Engagement</th><th>Jurisdiction</th><th>Status</th></tr></thead>\n"
     "      <tbody>\n"
     deliverable-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Consulting Engagement Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden by a human approver. Professional-standards spec-basis, evidence completeness, engagement-scope containment and conflict-of-interest status are independently recomputed, never trusted from the advisor's proposal; a real deliverable issuance is always a human consultant's call, at every rollout phase.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/deliverable-history db)) "deliverable drafts )")))
