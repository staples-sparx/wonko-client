(ns wonko-client.validation-test
  (:require [wonko-client.validation :as v]
            [clojure.test :refer :all]
            [wonko-client.test-fixtures :as tf]))

(use-fixtures :each tf/with-cleared-validation-state)

(deftest validate-label-names-test
  (testing "valid when label names have not changed"
    (is (v/valid? {:service "label-names" :metric-name "not-changed" :properties {"first" 1 "second" 2}}))
    (is (v/valid? {:service "label-names" :metric-name "not-changed" :properties {"first" 3 "second" 4}})))

  (testing "not valid when label names have been added"
    (is (v/valid? {:service "label-names" :metric-name "added" :properties {"first" 1}}))
    (is (not (v/valid? {:service "label-names" :metric-name "added" :properties {"first" 3 "second" 4}}))))

  (testing "not valid when label names have been removed"
    (is (v/valid? {:service "label-names" :metric-name "removed" :properties {"first" 3 "second" 4}}))
    (is (not (v/valid? {:service "label-names" :metric-name "removed" :properties {"first" 1}})))))
