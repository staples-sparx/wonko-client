(ns wonko-client.abq
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]
            [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util])
  (:import [java.util.concurrent ThreadPoolExecutor]
           [org.apache.kafka.clients.producer BufferExhaustedException]))

(defn send-sync [{:keys [topics producer] :as instance} topic message]
  (try
    (kp/send producer message (get topics topic))
    nil
    (catch BufferExhaustedException e
      (log/error e "unable to send cuz buffer full"))
    (catch Exception e
      (log/error e "unable to send"))))

(defn send-async [{^ThreadPoolExecutor thread-pool :queue :as instance} topic message]
  (.submit thread-pool ^Callable #(send-sync instance topic message))
  (log/debug :send-async)
  nil)

(defn init [options]
  (util/create-fixed-threadpool
   (set/rename-keys options {:worker-count :thread-pool-size})))

(defn terminate [{thread-pool :queue :as instance}]
  (util/stop-tp thread-pool))
