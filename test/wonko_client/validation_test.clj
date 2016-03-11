(ns wonko-client.validation-test
  (:require [wonko-client.validation :as v]
            [clojure.test :refer :all]
            [wonko-client.test-fixtures :as tf])
  (:import [clojure.lang ExceptionInfo]))

(use-fixtures :each tf/with-cleared-validation-state)

(deftest validate-label-names-test
  (testing "valid when label names have not changed"
    (is (nil? (v/validate! {:service "label-names" :metric-name "not-changed" :metric-value nil
                            :metric-type :counter :properties {"first" 1 "second" 2}})))
    (is (nil? (v/validate! {:service "label-names" :metric-name "not-changed"  :metric-value nil
                            :metric-type :counter :properties {"first" 3 "second" 4}}))))

  (testing "not valid when label names have been added"
    (is (nil? (v/validate! {:service "label-names" :metric-name "added" :metric-value nil
                            :metric-type :counter :properties {"first" 1}})))
    (is (thrown-with-msg?
         IllegalArgumentException #"Cannot change the label names for a metric."
         (v/validate! {:service "label-names" :metric-name "added" :metric-value nil
                       :metric-type :counter :properties {"first" 3 "second" 4}}))))

  (testing "not valid when label names have been removed"
    (is (nil? (v/validate! {:service "label-names" :metric-name "removed" :metric-value nil
                            :metric-type :counter :properties {"first" 3 "second" 4}})))
    (is (thrown-with-msg?
         IllegalArgumentException #"Cannot change the label names for a metric."
         (v/validate! {:service "label-names" :metric-name "removed" :metric-value nil
                       :metric-type :counter :properties {"first" 1}}))))

  (testing "metrics with the same name but different type are considered different metrics"
    (is (nil? (v/validate! {:service "label-names" :metric-name "removed" :metric-value nil
                            :metric-type :counter :properties {"first" 3 "second" 4}})))
    (is (nil? (v/validate! {:service "label-names" :metric-name "removed" :metric-value 3
                            :metric-type :gauge :properties {"first" 1}})))))

(deftest validate-metric-attributes
  (testing "metric value should be a number"
    (is (nil? (v/validate! {:service "test-service" :metric-name "test-metric" :metric-value 3
                            :metric-type :gauge :properties {"first" 3 "second" 4}})))
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name "test-metric" :metric-value "x"
                       :metric-type :gauge :properties {"first" 3 "second" 4}}))))

  (testing "property value should be a scalar, not a collection"
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name "test-metric" :metric-value 3
                       :metric-type :gauge :properties {"first" [3] "second" {:x 5}}}))))

  (testing "metric value for gauges and streams can not be nil"
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name "metric-value-test" :metric-value nil
                       :metric-type :gauge :properties {"first" 3 :second 4}})))
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name "metric-value-test" :metric-value nil
                       :metric-type :stream :properties {"first" 3 :second 4}})))
    (is (nil?
         (v/validate! {:service "test-service" :metric-name "metric-value-test" :metric-value nil
                       :metric-type :counter :properties {"first" 3 :second 4}}))))

  (testing "property names should be strings or keywords"
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name "prop-test-metric" :metric-value 3
                       :metric-type :gauge :properties {999 3 :second 4 "third" 5}})))
    (is (nil?
         (v/validate! {:service "test-service" :metric-name "prop-test-metric" :metric-value 3
                       :metric-type :gauge :properties {"first" 3 :second 4 "third" 5}}))))

  (testing "label values are not set when validation fails"
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name "fail-metric" :metric-value "x"
                       :metric-type :gauge :properties {"first" 3 "second" 4}})))
    (is (nil? (v/validate! {:service "test-service" :metric-name "fail-metric" :metric-value 3
                            :metric-type :gauge :properties {"third" 3 "second" 4}}))))

  (testing "metric-name should be valid"
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name 6 :metric-value 3
                       :metric-type :gauge :properties {"first" 3 "second" 4}})))
    (is (thrown?
         ExceptionInfo
         (v/validate! {:service "test-service" :metric-name :invalid-name? :metric-value 3
                       :metric-type :gauge :properties {"first" 3 "second" 4}})))))
