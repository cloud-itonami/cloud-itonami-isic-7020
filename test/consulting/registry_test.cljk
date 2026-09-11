(ns consulting.registry-test
  (:require [clojure.test :refer [deftest is]]
            [consulting.registry :as r]))

;; ----------------------------- engagement-scope-exceeded? -----------------------------

(deftest not-exceeded-when-recommendation-is-a-subset-of-contracted-scope
  (is (not (r/engagement-scope-exceeded? {:recommendation-topics #{:strategy}
                                          :contracted-scope-items #{:strategy :operations}})))
  (is (not (r/engagement-scope-exceeded? {:recommendation-topics #{:strategy :operations}
                                          :contracted-scope-items #{:strategy :operations}})))
  (is (not (r/engagement-scope-exceeded? {:recommendation-topics #{}
                                          :contracted-scope-items #{:strategy}}))))

(deftest exceeded-when-recommendation-includes-a-topic-outside-contracted-scope
  (is (r/engagement-scope-exceeded? {:recommendation-topics #{:strategy :governance}
                                     :contracted-scope-items #{:strategy :operations}}))
  (is (r/engagement-scope-exceeded? {:recommendation-topics #{:governance}
                                     :contracted-scope-items #{:strategy :operations}})))

(deftest exceeded-is-false-on-missing-fields
  (is (not (r/engagement-scope-exceeded? {})))
  (is (not (r/engagement-scope-exceeded? {:recommendation-topics #{:governance}}))))

;; ----------------------------- register-deliverable-issuance -----------------------------

(deftest issuance-is-a-draft-not-a-real-issuance
  (let [result (r/register-deliverable-issuance "engagement-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest issuance-assigns-issuance-number
  (let [result (r/register-deliverable-issuance "engagement-1" "JPN" 7)]
    (is (= (get result "issuance_number") "JPN-DLV-000007"))
    (is (= (get-in result ["record" "engagement_id"]) "engagement-1"))
    (is (= (get-in result ["record" "kind"]) "deliverable-issuance-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest issuance-validation-rules
  (is (thrown? Exception (r/register-deliverable-issuance "" "JPN" 0)))
  (is (thrown? Exception (r/register-deliverable-issuance "engagement-1" "" 0)))
  (is (thrown? Exception (r/register-deliverable-issuance "engagement-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-deliverable-issuance "engagement-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-deliverable-issuance "engagement-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-DLV-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-DLV-000001" (get-in hist2 [1 "record_id"])))))
