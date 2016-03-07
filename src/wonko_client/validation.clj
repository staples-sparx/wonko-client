(ns wonko-client.validation)

(defonce metric->label-names (atom {}))

(defn- maybe-set-label-names-for-metric [metric->label-names service metric-name label-names]
  (if (metric->label-names [service metric-name])
    metric->label-names
    (assoc metric->label-names [service metric-name] label-names)))

(defn valid? [{:keys [service metric-name properties] :as message}]
  (let [label-names (set (keys properties))]
    (swap! metric->label-names maybe-set-label-names-for-metric service metric-name label-names)
    (= label-names (@metric->label-names [service metric-name]))))

(defn validate! [message]
  (when-not (valid? message)
    (throw (IllegalArgumentException. "Cannot change the label names for a metric."))))
