(ns consulting.store
  "SSoT for the consulting actor, behind a `Store` protocol so the
  backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/consulting/store_contract_test.clj), which is the whole point:
  the actor, the Consulting Engagement Governor and the audit ledger
  never know which SSoT they run on.

  Single-actuation shape (one history collection, one sequence
  counter, one dedicated double-actuation-guard boolean) -- matching
  `leasing.store`'s/`underwriting.store`'s/`testlab.store`'s/
  `clinic.store`'s/`veterinary.store`'s/`funeral.store`'s/
  `parksafety.store`'s/`salon.store`'s/`entertainment.store`'s/
  `facility.store`'s single real-world actuation event (issuing a
  deliverable), with a dedicated `:deliverable-issued?` boolean (never
  a `:status` value) -- the same discipline every prior sibling
  governor's guards establish, informed by `cloud-itonami-isic-6492`'s
  status-lifecycle bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which engagement was
  screened for an unresolved conflict of interest, which deliverable
  was issued, on what jurisdictional basis, approved by whom' is
  always a query over an immutable log -- the audit trail a client
  trusting a consultancy needs, and the evidence a consultancy needs
  if a deliverable-issuance decision is later disputed."
  (:require [consulting.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (engagement [s id])
  (all-engagements [s])
  (conflict-screen-of [s engagement-id] "committed conflict-of-interest screening verdict for an engagement, or nil")
  (finding-of [s engagement-id] "committed finding/research record, or nil")
  (ledger [s])
  (deliverable-history [s] "the append-only deliverable-issuance history (consulting.registry drafts)")
  (next-deliverable-sequence [s jurisdiction] "next issuance-number sequence for a jurisdiction")
  (engagement-already-issued? [s engagement-id] "has this engagement's deliverable already been issued?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-engagements [s engagements] "replace/seed the engagement directory (map id->engagement)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained engagement set covering the actuation
  lifecycle (issuing a deliverable) so the actor + tests run
  offline."
  []
  {:engagements
   {"engagement-1" {:id "engagement-1" :client-name "Sakura Cooperative"
                     :contracted-scope-items #{:strategy :operations}
                     :recommendation-topics #{:strategy}
                     :conflict-of-interest-unresolved? false
                     :deliverable-issued? false
                     :jurisdiction "JPN" :status :intake}
    "engagement-2" {:id "engagement-2" :client-name "Atlantis Collective"
                     :contracted-scope-items #{:strategy :operations}
                     :recommendation-topics #{:strategy}
                     :conflict-of-interest-unresolved? false
                     :deliverable-issued? false
                     :jurisdiction "ATL" :status :intake}
    "engagement-3" {:id "engagement-3" :client-name "鈴木商店"
                     :contracted-scope-items #{:strategy :operations}
                     :recommendation-topics #{:strategy :governance}
                     :conflict-of-interest-unresolved? false
                     :deliverable-issued? false
                     :jurisdiction "JPN" :status :intake}
    "engagement-4" {:id "engagement-4" :client-name "田中商事"
                     :contracted-scope-items #{:strategy :operations}
                     :recommendation-topics #{:strategy}
                     :conflict-of-interest-unresolved? true
                     :deliverable-issued? false
                     :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-issuance!
  "Backend-agnostic `:engagement/mark-issued` -- looks up the
  engagement via the protocol and drafts the deliverable-issuance
  record, and returns {:result .. :engagement-patch ..} for the caller
  to persist."
  [s engagement-id]
  (let [e (engagement s engagement-id)
        seq-n (next-deliverable-sequence s (:jurisdiction e))
        result (registry/register-deliverable-issuance engagement-id (:jurisdiction e) seq-n)]
    {:result result
     :engagement-patch {:deliverable-issued? true
                        :issuance-number (get result "issuance_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (engagement [_ id] (get-in @a [:engagements id]))
  (all-engagements [_] (sort-by :id (vals (:engagements @a))))
  (conflict-screen-of [_ id] (get-in @a [:conflict-screens id]))
  (finding-of [_ engagement-id] (get-in @a [:findings engagement-id]))
  (ledger [_] (:ledger @a))
  (deliverable-history [_] (:deliverables @a))
  (next-deliverable-sequence [_ jurisdiction] (get-in @a [:deliverable-sequences jurisdiction] 0))
  (engagement-already-issued? [_ engagement-id] (boolean (get-in @a [:engagements engagement-id :deliverable-issued?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (swap! a update-in [:engagements (:id value)] merge value)

      :finding/set
      (swap! a assoc-in [:findings (first path)] payload)

      :conflict-screen/set
      (swap! a assoc-in [:conflict-screens (first path)] payload)

      :engagement/mark-issued
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (finalize-issuance! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:deliverable-sequences jurisdiction] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :deliverables registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-engagements [s engagements] (when (seq engagements) (swap! a assoc :engagements engagements)) s))

(defn seed-db
  "A MemStore seeded with the demo engagement set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :findings {} :conflict-screens {} :ledger [] :deliverable-sequences {}
                           :deliverables []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (finding/conflict-screen payloads, contracted-
  scope-items/recommendation-topics sets, ledger facts, deliverable
  records) are stored as EDN strings so `langchain.db` doesn't expand
  them into sub-entities -- the same convention every sibling actor's
  store uses."
  {:engagement/id                     {:db/unique :db.unique/identity}
   :finding/engagement-id              {:db/unique :db.unique/identity}
   :conflict-screen/engagement-id      {:db/unique :db.unique/identity}
   :ledger/seq                        {:db/unique :db.unique/identity}
   :deliverable/seq                   {:db/unique :db.unique/identity}
   :deliverable-sequence/jurisdiction {:db/unique :db.unique/identity}})

(defn- engagement->tx [{:keys [id client-name contracted-scope-items recommendation-topics
                               conflict-of-interest-unresolved?
                               deliverable-issued? jurisdiction status issuance-number]}]
  (cond-> {:engagement/id id}
    client-name                                (assoc :engagement/client-name client-name)
    contracted-scope-items                     (assoc :engagement/contracted-scope-items (ls/enc contracted-scope-items))
    recommendation-topics                      (assoc :engagement/recommendation-topics (ls/enc recommendation-topics))
    (some? conflict-of-interest-unresolved?)   (assoc :engagement/conflict-of-interest-unresolved? conflict-of-interest-unresolved?)
    (some? deliverable-issued?)                (assoc :engagement/deliverable-issued? deliverable-issued?)
    jurisdiction                               (assoc :engagement/jurisdiction jurisdiction)
    status                                     (assoc :engagement/status status)
    issuance-number                            (assoc :engagement/issuance-number issuance-number)))

(def ^:private engagement-pull
  [:engagement/id :engagement/client-name :engagement/contracted-scope-items :engagement/recommendation-topics
   :engagement/conflict-of-interest-unresolved? :engagement/deliverable-issued?
   :engagement/jurisdiction :engagement/status :engagement/issuance-number])

(defn- pull->engagement [m]
  (when (:engagement/id m)
    {:id (:engagement/id m) :client-name (:engagement/client-name m)
     :contracted-scope-items (ls/dec* (:engagement/contracted-scope-items m))
     :recommendation-topics (ls/dec* (:engagement/recommendation-topics m))
     :conflict-of-interest-unresolved? (boolean (:engagement/conflict-of-interest-unresolved? m))
     :deliverable-issued? (boolean (:engagement/deliverable-issued? m))
     :jurisdiction (:engagement/jurisdiction m) :status (:engagement/status m)
     :issuance-number (:engagement/issuance-number m)}))

(defrecord DatomicStore [conn]
  Store
  (engagement [_ id]
    (pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id id])))
  (all-engagements [_]
    (->> (d/q '[:find [?id ...] :where [?e :engagement/id ?id]] (d/db conn))
         (map #(pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id %])))
         (sort-by :id)))
  (conflict-screen-of [_ id]
    (ls/dec* (d/q '[:find ?p . :in $ ?eid
                :where [?k :conflict-screen/engagement-id ?eid] [?k :conflict-screen/payload ?p]]
              (d/db conn) id)))
  (finding-of [_ engagement-id]
    (ls/dec* (d/q '[:find ?p . :in $ ?eid
                :where [?a :finding/engagement-id ?eid] [?a :finding/payload ?p]]
              (d/db conn) engagement-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (deliverable-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :deliverable/seq ?s] [?e :deliverable/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (next-deliverable-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :deliverable-sequence/jurisdiction ?j] [?e :deliverable-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (engagement-already-issued? [s engagement-id]
    (boolean (:deliverable-issued? (engagement s engagement-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (d/transact! conn [(engagement->tx value)])

      :finding/set
      (d/transact! conn [{:finding/engagement-id (first path) :finding/payload (ls/enc payload)}])

      :conflict-screen/set
      (d/transact! conn [{:conflict-screen/engagement-id (first path) :conflict-screen/payload (ls/enc payload)}])

      :engagement/mark-issued
      (let [engagement-id (first path)
            {:keys [result engagement-patch]} (finalize-issuance! s engagement-id)
            jurisdiction (:jurisdiction (engagement s engagement-id))
            next-n (inc (next-deliverable-sequence s jurisdiction))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:deliverable-sequence/jurisdiction jurisdiction :deliverable-sequence/next next-n}
                      {:deliverable/seq (count (deliverable-history s)) :deliverable/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (ls/enc fact)}])
    fact)
  (with-engagements [s engagements]
    (when (seq engagements) (d/transact! conn (mapv engagement->tx (vals engagements)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:engagements ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [engagements]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-engagements s engagements))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo engagement set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
