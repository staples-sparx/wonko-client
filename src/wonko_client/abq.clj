(ns wonko-client.abq
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]
            [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util])
  (:import [java.util.concurrent ThreadPoolExecutor]))

(defn send-async [{^ThreadPoolExecutor thread-pool :queue :as instance} topic message]
  (.submit thread-pool ^Callable #(kp/send instance topic message))
  nil)

(defn init [options]
  (util/create-fixed-threadpool
   (set/rename-keys options {:worker-count :thread-pool-size})))

(defn terminate [{thread-pool :queue :as instance}]
  (util/stop-tp thread-pool))
