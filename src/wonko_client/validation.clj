(ns wonko-client.validation
  (:require [schema.core :as s]
            [clojure.tools.logging :as log]))

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

(defn valid-metric-name? [metric-name]
  (not (boolean (re-find #"[^A-z0-9\-_ ]" (name metric-name)))))

(def str-or-kw
  (s/cond-pre s/Str s/Keyword))

(def MessageSchema
  {:service  (s/constrained s/Str valid-metric-name?)
   :metadata {:host s/Str
              :ip-address s/Str
              :ts s/Num}
   :metric-name (s/constrained str-or-kw valid-metric-name?)
   :metric-type (s/enum :counter :stream :gauge)
   :metric-value s/Num
   :properties {str-or-kw (s/cond-pre s/Str s/Keyword s/Num)}
   (s/optional-key :options) {str-or-kw s/Any}})

(def CounterMessageSchema
  (assoc MessageSchema
    :metric-value (s/eq nil)))

(def AlertMessageSchema
  (assoc CounterMessageSchema
    :alert-name (s/constrained str-or-kw valid-metric-name?)
    :alert-info {s/Any s/Any}))

(def validators
  {:counter (s/validator CounterMessageSchema)
   :alert   (s/validator AlertMessageSchema)
   :gauge   (s/validator MessageSchema)
   :stream  (s/validator MessageSchema)})

(defn validate! [{:keys [service metric-name properties metric-type metric-value] :as message}]
  (when @validate?
    (let [validator (get validators metric-type)
          label-names (set (keys properties))
          existing-label-names (@metric->label-names (metric-key message))]
      (validator message)
      (when (and existing-label-names (not= existing-label-names label-names))
        (throw (IllegalArgumentException. "Cannot change the label names for a metric.")))
      (swap! metric->label-names maybe-set-label-names-for-metric message label-names)
      nil)))

(defn set-validation! [value]
  (reset! validate? value))
