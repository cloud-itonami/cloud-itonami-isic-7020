(ns consulting.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a configuration
  change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the sibling
  actor."
  (:require [clojure.test :refer [deftest is testing]]
            [consulting.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sakura Cooperative" (:client-name (store/engagement s "engagement-1"))))
      (is (= "JPN" (:jurisdiction (store/engagement s "engagement-1"))))
      (is (= #{:strategy :operations} (:contracted-scope-items (store/engagement s "engagement-1"))))
      (is (= #{:strategy} (:recommendation-topics (store/engagement s "engagement-1"))))
      (is (false? (:conflict-of-interest-unresolved? (store/engagement s "engagement-1"))))
      (is (= #{:strategy :governance} (:recommendation-topics (store/engagement s "engagement-3"))))
      (is (true? (:conflict-of-interest-unresolved? (store/engagement s "engagement-4"))))
      (is (false? (:deliverable-issued? (store/engagement s "engagement-1"))))
      (is (= ["engagement-1" "engagement-2" "engagement-3" "engagement-4"]
             (mapv :id (store/all-engagements s))))
      (is (nil? (store/conflict-screen-of s "engagement-1")))
      (is (nil? (store/finding-of s "engagement-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/deliverable-history s)))
      (is (zero? (store/next-deliverable-sequence s "JPN")))
      (is (false? (store/engagement-already-issued? s "engagement-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :engagement/upsert
                                 :value {:id "engagement-1" :client-name "Sakura Cooperative"}})
        (is (= "Sakura Cooperative" (:client-name (store/engagement s "engagement-1"))))
        (is (= #{:strategy} (:recommendation-topics (store/engagement s "engagement-1"))) "unrelated field preserved"))
      (testing "finding / conflict-screen payloads commit and read back"
        (store/commit-record! s {:effect :finding/set :path ["engagement-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/finding-of s "engagement-1")))
        (store/commit-record! s {:effect :conflict-screen/set :path ["engagement-1"]
                                 :payload {:engagement-id "engagement-1" :verdict :resolved}})
        (is (= {:engagement-id "engagement-1" :verdict :resolved} (store/conflict-screen-of s "engagement-1"))))
      (testing "deliverable issuance drafts a record and advances the sequence"
        (store/commit-record! s {:effect :engagement/mark-issued :path ["engagement-1"]})
        (is (= "JPN-DLV-000000" (get (first (store/deliverable-history s)) "record_id")))
        (is (= "deliverable-issuance-draft" (get (first (store/deliverable-history s)) "kind")))
        (is (true? (:deliverable-issued? (store/engagement s "engagement-1"))))
        (is (= 1 (count (store/deliverable-history s))))
        (is (= 1 (store/next-deliverable-sequence s "JPN")))
        (is (true? (store/engagement-already-issued? s "engagement-1")))
        (is (false? (store/engagement-already-issued? s "engagement-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/engagement s "nope")))
    (is (= [] (store/all-engagements s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/deliverable-history s)))
    (is (zero? (store/next-deliverable-sequence s "JPN")))
    (store/with-engagements s {"x" {:id "x" :client-name "n"
                                    :contracted-scope-items #{:strategy}
                                    :recommendation-topics #{:strategy}
                                    :conflict-of-interest-unresolved? false
                                    :deliverable-issued? false
                                    :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:client-name (store/engagement s "x"))))
    (is (= #{:strategy} (:contracted-scope-items (store/engagement s "x"))))))
