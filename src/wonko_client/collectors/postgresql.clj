(ns wonko-client.collectors.postgresql
  (:require [clojure.java.jdbc :as j]
            [clojure.string :as s]
            [wonko-client.core :as client]
            [wonko-client.collectors.postgresql.queries :as queries])
  (:import [java.sql SQLException]))

(defn ensure-extension-init [conn]
  (j/execute! conn ["CREATE EXTENSION IF NOT EXISTS pg_stat_statements"]))

(defn stats [get-conn-fn]
  (j/with-db-connection [conn (get-conn-fn)]
    (ensure-extension-init conn)
    (->> (for [[q-name {:keys [query properties]}] queries/all-queries]
           {q-name (vec (j/query conn query))})
         (into {}))))

(defn truncate-str [st]
  (apply str (take 150 st)))

(defn ->wonko-prop-value [v]
  (cond (number? v)  v
        (keyword? v) v
        (nil? v)     ""
        :else        (-> (str v)
                         (clojure.lang.Compiler/munge)
                         (s/replace #"[^\p{L}\p{Nd}]+" "_")
                         truncate-str)))

(defn row->wonko-metrics [q-name row]
  (let [property-names (get-in queries/all-queries [q-name :properties])
        properties (->> property-names
                        (map row)
                        (map ->wonko-prop-value)
                        (zipmap property-names))
        stats (apply dissoc row property-names)]
    (for [[stat-name stat-value] stats
          :when (some? stat-value)]
      {:metric-name (str "pg_" (s/replace (name q-name) #"-" "_"))
       :properties (assoc properties :stat-name (name stat-name))
       :metric-value stat-value})))

(defn send-metrics [get-conn-fn]
  (doseq [[q-name rows] (stats get-conn-fn)
          row rows
          {:keys [metric-name metric-value properties]} (row->wonko-metrics q-name row)]
    (client/gauge metric-name properties metric-value)))
