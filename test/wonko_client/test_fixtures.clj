(ns wonko-client.test-fixtures
  (:require [wonko-client.message.validation :as v]))

(defn with-cleared-validation-state [test-fn]
  (reset! v/metric->label-names {})
  (v/set-validation! true)
  (test-fn)
  (reset! v/metric->label-names {})
  (v/set-validation! false))
