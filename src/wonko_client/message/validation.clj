(ns wonko-client.message.validation
  (:require [wonko-client.message.schema :as schema]))

(defonce validate?
  (atom false))

(defonce metric->property-names
  (atom {}))

(defn- metric-key
  "A unique representation of a given metric, used as a key
  in `metric->property-names'"
  [{:keys [service metric-name metric-type] :as message}]
  [service metric-name metric-type])

(defn- maybe-set-property-names-for-metric
  [metric->property-names message property-names]
  (if (metric->property-names (metric-key message))
    metric->property-names
    (assoc metric->property-names (metric-key message) property-names)))

(defn validate-property-names [{:keys [properties] :as message}]
  (let [property-names (set (keys properties))
        existing-property-names (@metric->property-names (metric-key message))]
    (when (and existing-property-names (not= existing-property-names property-names))
      (throw (IllegalArgumentException. "Cannot change the property names for a metric.")))
    (swap! metric->property-names maybe-set-property-names-for-metric message property-names)))

(defn validate! [message]
  (when @validate?
    (schema/validate-schema message)
    (validate-property-names message)
    nil))

(defn set-validation! [value]
  (reset! validate? value))
