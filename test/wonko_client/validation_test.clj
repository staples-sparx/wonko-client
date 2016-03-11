(ns wonko-client.validation-test
  (:require [wonko-client.validation :as v]
            [clojure.test :refer :all]
            [wonko-client.test-fixtures :as tf]))

(use-fixtures :each tf/with-cleared-validation-state)

(deftest validate-label-names-test
  (testing "valid when label names have not changed"
    (is (nil? (v/validate! {:service "label-names" :metric-name "not-changed"
                            :metric-type :counter :properties {"first" 1 "second" 2}})))
    (is (nil? (v/validate! {:service "label-names" :metric-name "not-changed"
                            :metric-type :counter :properties {"first" 3 "second" 4}}))))

  (testing "not valid when label names have been added"
    (is (nil? (v/validate! {:service "label-names" :metric-name "added"
                            :metric-type :counter :properties {"first" 1}})))
    (is (thrown-with-msg?
         IllegalArgumentException #"Cannot change the label names for a metric."
         (v/validate! {:service "label-names" :metric-name "added"
                       :metric-type :counter :properties {"first" 3 "second" 4}}))))

  (testing "not valid when label names have been removed"
    (is (nil? (v/validate! {:service "label-names" :metric-name "removed"
                            :metric-type :counter :properties {"first" 3 "second" 4}})))
    (is (thrown-with-msg?
         IllegalArgumentException #"Cannot change the label names for a metric."
         (v/validate! {:service "label-names" :metric-name "removed"
                       :metric-type :counter :properties {"first" 1}}))))

  (testing "metrics with the same name but different type are considered different metrics"
    (is (nil? (v/validate! {:service "label-names" :metric-name "removed"
                            :metric-type :counter :properties {"first" 3 "second" 4}})))
    (is (nil? (v/validate! {:service "label-names" :metric-name "removed" :metric-value 3
                            :metric-type :gauge :properties {"first" 1}})))))

(deftest validate-metric-attributes
  (testing "metric value should be a number"
    (is (nil? (v/validate! {:service "test-service" :metric-name "test-metric" :metric-value 3
                            :metric-type :gauge :properties {"first" 3 "second" 4}})))
    (is (thrown-with-msg?
         IllegalArgumentException #"Metric value should be a number."
         (v/validate! {:service "test-service" :metric-name "test-metric" :metric-value "x"
                       :metric-type :gauge :properties {"first" 3 "second" 4}}))))

  (testing "property value should be a scalar, not a collection"
    (is (thrown-with-msg?
         IllegalArgumentException #"Property value should be a scalar, not a collection."
         (v/validate! {:service "test-service" :metric-name "test-metric" :metric-value 3
                       :metric-type :gauge :properties {"first" [3] "second" {:x 5}}}))))

  (testing "metric value for gauges and streams can not be nil"
    (is (thrown-with-msg?
         IllegalArgumentException #"Metric value for gauge can not be nil."
         (v/validate! {:service "test-service" :metric-name "metric-value-test" :metric-value nil
                       :metric-type :gauge :properties {"first" 3 :second 4}})))
    (is (thrown-with-msg?
         IllegalArgumentException #"Metric value for stream can not be nil."
         (v/validate! {:service "test-service" :metric-name "metric-value-test" :metric-value nil
                       :metric-type :stream :properties {"first" 3 :second 4}})))

    (is (nil?
         (v/validate! {:service "test-service" :metric-name "metric-value-test" :metric-value nil
                       :metric-type :counter :properties {"first" 3 :second 4}}))))

  (testing "property names should be strings or keywords"
    (is (thrown-with-msg?
         IllegalArgumentException #"Property names have to be strings or keywords."
         (v/validate! {:service "test-service" :metric-name "prop-test-metric" :metric-value 3
                       :metric-type :gauge :properties {999 3 :second 4 "third" 5}})))
    (is (nil?
         (v/validate! {:service "test-service" :metric-name "prop-test-metric" :metric-value 3
                       :metric-type :gauge :properties {"first" 3 :second 4 "third" 5}}))))

  (testing "label values are not set when validation fails"
    (is (thrown-with-msg?
         IllegalArgumentException #"Metric value should be a number."
         (v/validate! {:service "test-service" :metric-name "fail-metric" :metric-value "x"
                       :metric-type :gauge :properties {"first" 3 "second" 4}})))

    (is (nil? (v/validate! {:service "test-service" :metric-name "fail-metric" :metric-value 3
                            :metric-type :gauge :properties {"third" 3 "second" 4}})))))
