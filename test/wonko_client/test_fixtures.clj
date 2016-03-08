(ns wonko-client.test-fixtures
  (:require [wonko-client.validation :as v]))

(defn with-cleared-validation-state [test-fn]
  (reset! v/metric->label-names {})
  (test-fn)
  (reset! v/metric->label-names {}))

