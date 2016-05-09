(ns wonko-client.collectors.postgresql
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:import [java.sql SQLException]))

(defonce conn
  (atom nil))

(defn read-prepared-stmts []
  (-> "monitor-postgresql.sql" io/resource slurp))

(defn init [get-conn-fn]
  (reset! conn {:connection (get-conn-fn)})
  (j/execute! @conn (read-prepared-stmts))
  nil)

(defn stats []
  (let [stmts (j/query @conn "select * from pg_prepared_statements where from_sql is true")]
    (->> (for [stmt stmts
               :let [q-name (:name stmt)
                     q (str "EXECUTE " q-name)]]
           {q-name (vec (j/query @conn q))})
         (into {}))))


(defn ->wonko-name [query-str]
  (if (some? query-str)
    (-> query-str
        (clojure.lang.Compiler/munge)
        (s/replace #"[^\p{L}\p{Nd}]+" "_"))
    ""))

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
   "table_bloat" #{:relation :is_na}})

(defn row->wonko-metrics [category row]
  (let [property-names (get category-properties category)
        properties (->> property-names
                        (map row)
                        (map ->wonko-name)
                        (zipmap property-names))
        stats (apply dissoc row property-names)]
    (for [[stat-name stat-value] stats
          :when (some? stat-value)]
      {:metric-name (str "pg_" category)
       :properties (assoc properties :stat-name (name stat-name))
       :metric-value stat-value})))

(defn send-metrics []
  (doall
   (for [[category rows] (stats)
         row rows
         :let [{:keys [metric-name metric-value properties]}
               (row->wonko-metrics category row)]]
     (client/gauge metric-name properties metric-value))))
