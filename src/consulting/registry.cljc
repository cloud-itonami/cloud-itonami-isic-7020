(ns consulting.registry
  "Pure-function deliverable-issuance record construction -- an
  append-only management-consultancy book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a deliverable-issuance
  reference number -- every consultancy/jurisdiction assigns its own
  reference format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `consulting.facts` uses.

  `engagement-scope-exceeded?` is the FOURTH instance of this fleet's
  set-containment/subset check family (`registrar.registry/
  prerequisites-satisfied?` established the first, `casework.registry/
  eligibility-criteria-unsatisfied?` the second, `secondary.registry/
  graduation-requirements-unsatisfied?` the third), applying the SAME
  subset-comparison shape to an engagement's own recorded
  `:recommendation-topics` against its own recorded `:contracted-
  scope-items` -- but in the OPPOSITE polarity from its predecessors
  (a REQUIRED set must be a subset of a SATISFIED set there; here a
  PROPOSED set must stay a subset of a CONTRACTED set) -- the same
  underlying `clojure.set/subset?` mechanism generalizing to a
  'boundary/permission' framing rather than a 'sufficiency' framing.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real engagement-management system. It builds the RECORD
  a consultancy would keep, not the act of issuing the deliverable
  itself (that is `consulting.operation`'s `:actuation/issue-
  deliverable`, always human-gated -- see README `Actuation`)."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  consultancy's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn engagement-scope-exceeded?
  "Does `engagement`'s own `:recommendation-topics` set contain any
  topic NOT in its own `:contracted-scope-items` set? A pure ground-
  truth check against the engagement's own permanent fields -- no
  upstream comparison needed. The FOURTH instance of this fleet's
  set-containment/subset check family (see ns docstring)."
  [{:keys [recommendation-topics contracted-scope-items]}]
  (and (set? recommendation-topics) (set? contracted-scope-items)
       (not (set/subset? recommendation-topics contracted-scope-items))))

(defn register-deliverable-issuance
  "Validate + construct the DELIVERABLE-ISSUANCE registration DRAFT --
  the consultancy's own act of issuing a real recommendation/
  deliverable to a client. Pure function -- does not touch any real
  engagement-management system; it builds the RECORD a consultancy
  would keep. `consulting.governor` independently re-verifies the
  engagement's own scope sufficiency against its own contracted-scope
  bounds, and blocks a double-issuance for the same engagement, before
  this is ever allowed to commit."
  [engagement-id jurisdiction sequence]
  (when-not (and engagement-id (not= engagement-id ""))
    (throw (ex-info "deliverable-issuance: engagement_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "deliverable-issuance: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "deliverable-issuance: sequence must be >= 0" {})))
  (let [issuance-number (str (str/upper-case jurisdiction) "-DLV-" (zero-pad sequence 6))
        record {"record_id" issuance-number
                "kind" "deliverable-issuance-draft"
                "engagement_id" engagement-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "issuance_number" issuance-number
     "certificate" (unsigned-certificate "DeliverableIssuance" issuance-number issuance-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
