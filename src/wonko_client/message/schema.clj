(ns wonko-client.message.schema
  (:require [schema.core :as s]
            [schema.spec.core :as spec]
            [schema.spec.leaf :as leaf]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom schemas to enhance error readability.

(defrecord StringOrKeywordSchema []
  s/Schema
  (spec [this] (leaf/leaf-spec
                (spec/precondition this
                                   #(or (string? %) (keyword? %))
                                   #(list 'string-or-keyword %))))
  (explain [this] (list 'string-or-keyword)))

(def StringOrKeyword
  (StringOrKeywordSchema.))

(defrecord StringKeywordOrNumberSchema []
  s/Schema
  (spec [this] (leaf/leaf-spec
                (spec/precondition this
                                   #(or (string? %) (keyword? %) (number? %))
                                   #(list 'string-keyword-or-number %))))
  (explain [this] (list 'string-keyword-or-number)))

(def StringKeywordOrNumber
  (StringKeywordOrNumberSchema.))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema for wonko messages

(defn valid-metric-name? [metric-name]
  (not (boolean (re-find #"[^A-z0-9\-_ ]" (name metric-name)))))

(def MessageSchema
  {:service  (s/constrained s/Str valid-metric-name?)
   :metadata {:host s/Str
              :ip-address s/Str
              :ts s/Num}
   :metric-name (s/constrained StringOrKeyword valid-metric-name?)
   :metric-type (s/enum :counter :stream :gauge)
   :metric-value s/Num
   :properties (s/maybe {StringOrKeyword StringKeywordOrNumber})
   (s/optional-key :options) (s/maybe {StringOrKeyword s/Any})})

(def CounterMessageSchema
  (assoc MessageSchema
    :metric-value (s/eq nil)))

(def AlertMessageSchema
  (assoc CounterMessageSchema
    :alert-name (s/constrained StringOrKeyword valid-metric-name?)
    :alert-info {s/Any s/Any}))

(def validators
  {:counter (s/validator CounterMessageSchema)
   :alert   (s/validator AlertMessageSchema)
   :gauge   (s/validator MessageSchema)
   :stream  (s/validator MessageSchema)})

(defn validate-schema [{:keys [alert-name metric-type] :as message}]
  (let [effective-metric-type (if (some? alert-name) :alert metric-type)
        validator (get validators effective-metric-type)]
    (if validator
      (validator message)
      (throw (IllegalArgumentException. "Invalid :metric-type.")))))
