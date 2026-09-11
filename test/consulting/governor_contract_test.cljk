(ns consulting.governor-contract-test
  "The governor contract as executable tests -- the management-
  consultancy analog of `cloud-itonami-isic-6512`'s `casualty.
  governor-contract-test`. The single invariant under test:

    Consultant-LLM never issues a deliverable the Consulting
    Engagement Governor would reject, `:actuation/issue-deliverable`
    NEVER auto-commits at any phase, `:engagement/intake` (no direct
    capital risk) MAY auto-commit when clean, and every decision
    (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [consulting.store :as store]
            [consulting.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :consultant :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- research!
  "Walks `subject` through research -> approve, leaving a finding on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-research") {:op :finding/research :subject subject} operator)
  (approve! actor (str tid-prefix "-research")))

(defn- screen!
  "Walks `subject` through conflict-of-interest screening -> approve,
  leaving a screening on file. Only safe to call for an engagement
  whose conflict status has already resolved -- an unresolved conflict
  HARD-holds the screen itself (see
  `conflict-of-interest-is-held-and-unoverridable`)."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-screen") {:op :conflict/screen :subject subject} operator)
  (approve! actor (str tid-prefix "-screen")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :engagement/intake :subject "engagement-1"
                   :patch {:id "engagement-1" :client-name "Sakura Cooperative"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sakura Cooperative" (:client-name (store/engagement db "engagement-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest finding-research-always-needs-approval
  (testing "research is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :finding/research :subject "engagement-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/finding-of db "engagement-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a finding/research proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :finding/research :subject "engagement-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/finding-of db "engagement-1")) "no finding written"))))

(deftest issue-deliverable-without-research-is-held
  (testing "actuation/issue-deliverable before any finding research -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/issue-deliverable :subject "engagement-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest engagement-scope-exceeded-is-held
  (testing "an engagement whose own recommendation topics exceed its own contracted scope -> HOLD"
    (let [[db actor] (fresh)
          _ (research! actor "t5pre" "engagement-3")
          res (exec-op actor "t5" {:op :actuation/issue-deliverable :subject "engagement-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:engagement-scope-exceeded} (-> (store/ledger db) last :basis)))
      (is (empty? (store/deliverable-history db))))))

(deftest conflict-of-interest-is-held-and-unoverridable
  (testing "an unresolved conflict of interest on an engagement -> HOLD, and never reaches request-approval -- exercised via :conflict/screen DIRECTLY, not via the actuation op against an unscreened engagement (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's and recovery's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :conflict/screen :subject "engagement-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:conflict-of-interest-unresolved} (-> (store/ledger db) first :basis)))
      (is (nil? (store/conflict-screen-of db "engagement-4")) "no clearance written"))))

(deftest issue-deliverable-always-escalates-then-human-decides
  (testing "a clean, fully-researched, in-scope, conflict-cleared engagement still ALWAYS interrupts for human approval -- actuation/issue-deliverable is never auto"
    (let [[db actor] (fresh)
          _ (research! actor "t7pre" "engagement-1")
          _ (screen! actor "t7pre2" "engagement-1")
          r1 (exec-op actor "t7" {:op :actuation/issue-deliverable :subject "engagement-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, deliverable record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:deliverable-issued? (store/engagement db "engagement-1"))))
          (is (= 1 (count (store/deliverable-history db))) "one draft deliverable record"))))))

(deftest issue-deliverable-double-issuance-is-held
  (testing "issuing the same engagement's deliverable twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (research! actor "t8pre" "engagement-1")
          _ (exec-op actor "t8a" {:op :actuation/issue-deliverable :subject "engagement-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/issue-deliverable :subject "engagement-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-issued} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/deliverable-history db))) "still only the one earlier issuance"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :engagement/intake :subject "engagement-1"
                          :patch {:id "engagement-1" :client-name "Sakura Cooperative"}} operator)
      (exec-op actor "b" {:op :finding/research :subject "engagement-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
