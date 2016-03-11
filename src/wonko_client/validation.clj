(ns wonko-client.validation
  (:require [clojure.tools.logging :as log]))

(defonce metric->label-names (atom {}))

(defn- metric-key
  "A unique representation of a given metric, used as a key
   in `metric->label-names'"
  [{:keys [service metric-name metric-type] :as message}]
  [service metric-name metric-type])

(defn- maybe-set-label-names-for-metric
  [metric->label-names message label-names]
  (if (metric->label-names (metric-key message))
    metric->label-names
    (assoc metric->label-names (metric-key message) label-names)))

(defn valid? [{:keys [service metric-name properties metric-type] :as message}]
  (let [label-names (set (keys properties))]
    (swap! metric->label-names maybe-set-label-names-for-metric message label-names)
    (= label-names (@metric->label-names (metric-key message)))))

(defn validate! [message]
  (when-not (valid? message)
    (let [ex (IllegalArgumentException. "Cannot change the label names for a metric.")]
      (log/error ex "Validation failed.")
      (throw ex))))
