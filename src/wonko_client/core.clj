(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util]
            [wonko-client.message :as message]
            [wonko-client.message.validation :as v]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent ThreadPoolExecutor]
           [com.codahale.metrics MetricRegistry Timer]
           [org.apache.kafka.clients.producer BufferExhaustedException]))

(def ^:private default-options
  {:validate?        false
   :drop-on-reject?  false
   :thread-pool-size 10
   :queue-size       10
   :topics           {:events "wonko-events"
                      :alerts "wonko-alerts"}})
(defonce instance
  {:service nil
   :topics nil
   :thread-pool nil
   :producer nil})

(def ^MetricRegistry metrics)

(def ^Timer send-sync-timer)

(def ^Timer send-async-timer)

(defn metrics-init []
  (alter-var-root #'metrics (constantly (MetricRegistry.)))
  (alter-var-root #'send-sync-timer (constantly (.timer metrics "send-sync")))
  (alter-var-root #'send-async-timer (constantly (.timer metrics "send-async"))))

(defn- send-sync [{:keys [topics producer] :as instance} topic message]
  (let [context (.time send-sync-timer)]
    (try
      (kp/send producer message (get topics topic))
      (catch BufferExhaustedException e
        (log/error e "unable to send cuz buffer full"))
      (catch Exception e
        (log/error e "unable to send")))
    (.stop context)))

(defn- send-async [{:keys [^ThreadPoolExecutor thread-pool] :as instance} topic message]
  (let [context (.time send-async-timer)]
    (.submit thread-pool ^Callable #(send-sync instance topic message))
    (.stop context))
  (log/debug :send-async))

(defn counter [metric-name properties & {:as options}]
  (->> :counter
       (message/build (:service instance) metric-name properties nil options)
       (send-async instance :events)))

(defn gauge [metric-name properties metric-value & {:as options}]
  (->> :gauge
       (message/build (:service instance) metric-name properties metric-value options)
       (send-async instance :events)))

(defn stream [metric-name properties metric-value & {:as options}]
  (->> :stream
       (message/build (:service instance) metric-name properties metric-value options)
       (send-async instance :events)))

(defn alert [alert-name alert-info]
  (->> :counter
       (message/build-alert (:service instance) alert-name {} nil alert-name alert-info nil)
       (send-sync instance :alerts)))

(defn set-topics! [events-topic alerts-topic]
  (swap! instance assoc :topics {:events events-topic
                                 :alerts alerts-topic}))

(defn init! [service-name kafka-config & {:as user-options}]
  (let [options (merge default-options user-options)
        {:keys [validate? thread-pool-size queue-size topics]} options]
    (alter-var-root #'instance
                    (constantly
                     {:service service-name
                      :topics topics
                      :thread-pool (util/create-fixed-threadpool options)
                      :producer (kp/create kafka-config options)}))
    (v/set-validation! validate?)
    (log/info "wonko-client initialized" instance)
    nil))

(defn terminate! []
  (kp/close (:producer instance))
  (util/stop-tp (:thread-pool instance))
  nil)
