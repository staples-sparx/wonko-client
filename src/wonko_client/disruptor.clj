(ns wonko-client.disruptor
  (:require
   [wonko-client.kafka-producer :as kp]
   [clojure.tools.logging :as log]
   [wonko-client.util :as util])
  (:import
   [com.lmax.disruptor.dsl Disruptor ProducerType]
   [java.util.concurrent Executors]
   [com.lmax.disruptor EventFactory EventHandler BlockingWaitStrategy RingBuffer WorkerPool IgnoreExceptionHandler WorkHandler]
   [wonko_client.disruptor Event]
   [org.apache.kafka.clients.producer BufferExhaustedException]
   [com.codahale.metrics MetricRegistry Timer ExponentiallyDecayingReservoir]))

(def ^MetricRegistry metrics)

(def ^Timer send-sync-timer)

(def ^Timer send-async-timer)

(defn metrics-init []
  (alter-var-root #'metrics (constantly (MetricRegistry.)))
  (alter-var-root #'send-sync-timer (constantly (util/create-timer metrics "d-send-sync")))
  (alter-var-root #'send-async-timer (constantly (util/create-timer metrics "d-send-async"))))

(defn- send-sync [{:keys [topics producer] :as instance} topic message]
  (util/with-time send-sync-timer
    (try
      (kp/send producer message (get topics topic))
      (catch BufferExhaustedException e
        (log/error e "unable to send cuz buffer full"))
      (catch Exception e
        (log/error e "unable to send")))))

(defn make-work-handler [instance topic]
  (proxy [WorkHandler] []
    (onEvent [event]
      (send-sync instance topic (.value event)))))

(defn make-event-factory []
  (proxy [EventFactory] []
    (newInstance [] (Event.))))

(defn make-disruptor [buffer-size executor]
  (Disruptor. (make-event-factory)
              (int buffer-size)
              executor
              ProducerType/MULTI
              (BlockingWaitStrategy.)))

(defn make-worker-pool [instance topic work-handlers]
  (WorkerPool. (make-event-factory)
               (IgnoreExceptionHandler.)
               work-handlers))

(defn send-async [d message]
  (util/with-time send-async-timer
    (let [rb (.getRingBuffer d)
          seq (.next rb)]
      (try
        (let [ev (.get rb seq)]
          (.set ev message))
        (finally
          (.publish rb seq))))))

(defn init [instance buffer-size]
  (let [executor (Executors/newCachedThreadPool)
        disruptor (make-disruptor buffer-size executor)
        topic :events
        work-handlers (into-array (repeatedly 10 #(make-work-handler instance topic)))
        worker-pool (make-worker-pool instance topic work-handlers)
        rb (.start worker-pool executor)]
    (.handleEventsWithWorkerPool disruptor work-handlers)
    (.start disruptor)
    {:disruptor disruptor
     :disruptor-executor executor}))

(defn terminate [{:keys [disruptor disruptor-executor] :as instance}]
  (.shutdown disruptor)
  (.shutdownNow disruptor-executor))
