(ns wonko-client.message.validation
  (:require [wonko-client.message.schema :as schema]))

(defonce validate?
  (atom false))

(defonce metric->label-names
  (atom {}))

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

(defn validate-label-names [{:keys [properties] :as message}]
  (let [label-names (set (keys properties))
        existing-label-names (@metric->label-names (metric-key message))]
    (when (and existing-label-names (not= existing-label-names label-names))
      (throw (IllegalArgumentException. "Cannot change the label names for a metric.")))
    (swap! metric->label-names maybe-set-label-names-for-metric message label-names)))

(defn validate! [message]
  (when @validate?
    (schema/validate-schema message)
    (validate-label-names message)
    nil))

(defn set-validation! [value]
  (reset! validate? value))
