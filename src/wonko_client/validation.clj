(ns wonko-client.validation)

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

(defn validate* [{:keys [service metric-name properties metric-type metric-value] :as message}]
  (let [label-names (@metric->label-names (metric-key message))]
    (cond (and label-names (not= label-names (set (keys properties))))
          "Cannot change the label names for a metric."

          (and (#{:gauge :stream} metric-type) (nil? metric-value))
          (format "Metric value for %s can not be nil." (name metric-type))

          (and metric-value (not (number? metric-value)))
          "Metric value should be a number."

          (some coll? (vals properties))
          "Property value should be a scalar, not a collection."

          (some #(not (or (string? %) (keyword? %))) (keys properties))
          "Property names have to be strings or keywords."

          (not (or (string? metric-name) (keyword? metric-name)))
          "Metric name should be a string or keyword."

          (re-find #"[^A-z0-9\-_ ]" (name metric-name))
          "Metric name can have alphanumeric, '-' and '_' characters only.")))

(defn validate! [message]
  (if-let [error-message (validate* message)]
    (throw (IllegalArgumentException. error-message))
    (let [label-names (-> message :properties keys set)]
      (swap! metric->label-names maybe-set-label-names-for-metric message label-names)
      nil)))
