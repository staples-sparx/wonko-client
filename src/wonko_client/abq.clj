(ns wonko-client.abq
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]
            [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util])
  (:import [com.codahale.metrics MetricRegistry Timer ExponentiallyDecayingReservoir]
           [java.util.concurrent ThreadPoolExecutor]
           [org.apache.kafka.clients.producer BufferExhaustedException]))

(def ^MetricRegistry metrics)
(def ^Timer send-sync-timer)
(def ^Timer send-async-timer)

(defn send-sync [{:keys [topics producer] :as instance} topic message]
  (util/with-time send-sync-timer
    (try
      (kp/send producer message (get topics topic))
      nil
      (catch BufferExhaustedException e
        (log/error e "unable to send cuz buffer full"))
      (catch Exception e
        (log/error e "unable to send")))))

(defn send-async [{^ThreadPoolExecutor thread-pool :queue :as instance} topic message]
  (util/with-time send-async-timer
    (.submit thread-pool ^Callable #(send-sync instance topic message)))
  (log/debug :send-async)
  nil)

(defn metrics-init []
  (alter-var-root #'metrics (constantly (MetricRegistry.)))
  (alter-var-root #'send-sync-timer (constantly (util/create-timer metrics "send-sync")))
  (alter-var-root #'send-async-timer (constantly (util/create-timer metrics "send-async"))))

(defn init [options]
  (metrics-init)
  (util/create-fixed-threadpool
   (set/rename-keys options {:worker-count :thread-pool-size})))

(defn terminate [{thread-pool :queue :as instance}]
  (util/stop-tp thread-pool))
