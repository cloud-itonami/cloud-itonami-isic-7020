(ns consulting.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean engagement
  through intake -> finding/research -> conflict-of-interest screening
  -> deliverable-issuance proposal (always escalates) -> human
  approval -> commit, then shows four HARD holds (a jurisdiction with
  no spec-basis, a deliverable whose recommendation topics exceed the
  contracted scope, an unresolved conflict of interest screened
  directly via `:conflict/screen` [never via an actuation op against
  an unscreened engagement -- see this actor's own governor ns
  docstring / the lesson `parksafety`'s ADR-2607071922 Decision 5,
  `eldercare`'s, `museum`'s, `conservation`'s, `salon`'s,
  `entertainment`'s, `casework`'s, `hospital`'s, `facility`'s,
  `school`'s, `association`'s, `leasing`'s, `behavioral`'s,
  `secondary`'s, `card`'s, `water`'s, `telecom`'s, `aerospace`'s and
  `recovery`'s ADR-0001s already recorded], and a double deliverable
  issuance of an already-processed engagement) that never reach a
  human at all, and prints the audit ledger + the draft deliverable-
  issuance records."
  (:require [langgraph.graph :as g]
            [consulting.store :as store]
            [consulting.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :consultant :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== engagement/intake engagement-1 (JPN, clean; recommendation within contracted scope, no COI) ==")
    (println (exec! actor "t1" {:op :engagement/intake :subject "engagement-1"
                                :patch {:id "engagement-1" :client-name "Sakura Cooperative"}} operator))

    (println "== finding/research engagement-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :finding/research :subject "engagement-1"} operator))
    (println (approve! actor "t2"))

    (println "== conflict/screen engagement-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :conflict/screen :subject "engagement-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/issue-deliverable engagement-1 (always escalates -- actuation/issue-deliverable) ==")
    (let [r (exec! actor "t4" {:op :actuation/issue-deliverable :subject "engagement-1"} operator)]
      (println r)
      (println "-- human consultant approves --")
      (println (approve! actor "t4")))

    (println "== finding/research engagement-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :finding/research :subject "engagement-2" :no-spec? true} operator))

    (println "== finding/research engagement-3 (escalates -- human approves; sets up the scope-exceeded test) ==")
    (println (exec! actor "t6" {:op :finding/research :subject "engagement-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/issue-deliverable engagement-3 (recommendation includes :governance, outside contracted scope -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/issue-deliverable :subject "engagement-3"} operator))

    (println "== conflict/screen engagement-4 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :conflict/screen :subject "engagement-4"} operator))

    (println "== actuation/issue-deliverable engagement-1 AGAIN (double-issuance -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/issue-deliverable :subject "engagement-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft deliverable-issuance records ==")
    (doseq [r (store/deliverable-history db)] (println r))))
