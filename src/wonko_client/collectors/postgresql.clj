(ns wonko-client.collectors.postgresql
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as j]
            [clojure.string :as s]
            [wonko-client.core :as client])
  (:import [java.sql SQLException]))

(def prepared-stmts-str
  (-> "monitor-postgresql.sql" io/resource slurp))

(def category-properties
  {"queries" #{:query}
   "index_usage" #{:relation :indexname}
   "relation_sizes" #{:relation}
   "tuple_info" #{:relation}
   "table_and_index_bloat" #{:relation :iname}
   "db_size" #{}
   "cache_total" #{}
   "table_sizes" #{:relation}
   "cache_tables" #{:relation}
   "table_bloat" #{:relation :is_na}
   "conn_states" #{:state}
   "conn_waits" #{:waiting}
   "conn_counts" #{:datname}})

(defn stats [get-conn-fn]
  (with-open [raw-conn (get-conn-fn)]
    (let [conn {:connection raw-conn}]
      (j/execute! conn [prepared-stmts-str])
      (->> (for [stmt (j/query conn "SELECT * FROM pg_prepared_statements WHERE from_sql IS true")
                 :let [q-name (:name stmt)
                       q (str "EXECUTE " q-name)]]
             {q-name (vec (j/query conn q))})
           (into {})))))

(defn ->wonko-prop-value [v]
  (cond (number? v)  v
        (keyword? v) v
        (nil? v)     ""
        :else        (-> (str v)
                         (clojure.lang.Compiler/munge)
                         (s/replace #"[^\p{L}\p{Nd}]+" "_"))))

(defn row->wonko-metrics [category row]
  (let [property-names (get category-properties category)
        properties (->> property-names
                        (map row)
                        (map ->wonko-prop-value)
                        (zipmap property-names))
        stats (apply dissoc row property-names)]
    (for [[stat-name stat-value] stats
          :when (some? stat-value)]
      {:metric-name (str "pg_" category)
       :properties (assoc properties :stat-name (name stat-name))
       :metric-value stat-value})))

(defn send-metrics [get-conn-fn]
  (doseq [[category rows] (stats get-conn-fn)
          row rows
          {:keys [metric-name metric-value properties]} (row->wonko-metrics category row)]
    (client/gauge metric-name properties metric-value)))
