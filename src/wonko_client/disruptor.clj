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

(defn send-sync [{:keys [topics producer] :as instance} topic message]
  (util/with-time send-sync-timer
    (try
      (kp/send producer message (get topics topic))
      (catch BufferExhaustedException e
        (log/error e "unable to send cuz buffer full"))
      (catch Exception e
        (log/error e "unable to send")))))

(defn send-async [{:keys [queue] :as instance} topic message]
  (util/with-time send-async-timer
    (let [rb (.getRingBuffer (:disruptor queue))
          seq (.next rb)]
      (try
        (let [ev (.get rb seq)]
          (.set ev {:instance instance :topic topic :message message}))
        (finally
          (.publish rb seq))))))

(defn make-event-factory []
  (proxy [EventFactory] []
    (newInstance [] (Event.))))

(defn make-disruptor [queue-size executor]
  (let [buffer-size (util/round-up-to-power-of-2 queue-size)]
    (Disruptor. (make-event-factory)
                buffer-size
                executor
                ProducerType/MULTI
                (BlockingWaitStrategy.))))

(defn make-work-handler []
  (proxy [WorkHandler] []
    (onEvent [event]
      (let [{:keys [instance topic message]} (.value event)]
        (send-sync instance topic message)))))

(defn make-worker-pool [work-handlers]
  (WorkerPool. (make-event-factory)
               (IgnoreExceptionHandler.)
               work-handlers))

(defn metrics-init []
  (alter-var-root #'metrics (constantly (MetricRegistry.)))
  (alter-var-root #'send-sync-timer (constantly (util/create-timer metrics "d-send-sync")))
  (alter-var-root #'send-async-timer (constantly (util/create-timer metrics "d-send-async"))))

(defn init [{:keys [worker-count queue-size] :as options}]
  ;; TODO: Implment the drop-on-reject? option
  (let [executor (Executors/newCachedThreadPool)
        disruptor (make-disruptor queue-size executor)
        work-handlers (into-array (repeatedly worker-count make-work-handler))
        worker-pool (make-worker-pool work-handlers)
        rb (.start worker-pool executor)]
    (.handleEventsWithWorkerPool disruptor work-handlers)
    (.start disruptor)
    (metrics-init)
    {:disruptor disruptor
     :worker-pool worker-pool
     :disruptor-executor executor}))

(defn terminate [{:keys [queue] :as instance}]
  (let [{:keys [disruptor disruptor-executor]} queue]
    (.shutdown disruptor)
    (.shutdownNow disruptor-executor)))
