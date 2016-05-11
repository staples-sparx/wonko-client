(ns wonko-client.collectors.postgresql
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [wonko-client.core :as client])
  (:import [java.sql SQLException]))

(def prepared-stmts-str
  (-> "monitor-postgresql.sql" io/resource slurp))

(def get-conn-fn
  "The function given by the application used to attain a connection"
  nil)

(defn init [get-conn-fn-input]
  (alter-var-root #'get-conn-fn (constantly get-conn-fn-input))
  nil)

(defn stats []
  (with-open [raw-conn (get-conn-fn)]
    (let [conn {:connection raw-conn}]
      (j/execute! conn prepared-stmts-str)
      (->> (for [stmt (j/query conn "SELECT * FROM pg_prepared_statements WHERE from_sql IS true")
                 :let [q-name (:name stmt)
                       q (str "EXECUTE " q-name)]]
             {q-name (vec (j/query conn q))})
           (into {})))))

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
         {:keys [metric-name metric-value properties]} (row->wonko-metrics category row)]
     (client/gauge metric-name properties metric-value))))
