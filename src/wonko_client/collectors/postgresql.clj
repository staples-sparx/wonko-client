(ns wonko-client.collectors.postgresql
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io])
  (:import [java.sql SQLException]))

(defonce conn
  (atom nil))

(defn read-prepared-stmts []
  (-> "monitor-postgresql.sql" io/resource slurp))

(defn init [get-conn-fn]
  (reset! conn {:connection (get-conn-fn)})
  (j/execute! @conn (read-prepared-stmts))
  nil)

(defn get-data []
  (let [stmts (j/query @conn "select * from pg_prepared_statements where from_sql is true")]
    (into {}
          (for [stmt stmts
                :let [q-name (:name stmt)
                      q (str "EXECUTE " q-name)]]
            {q-name (j/query @conn q)}))))

(defn send-metrics []
  nil)
