(ns wonko-client.disruptor
  (:require [clojure.tools.logging :as log]
            [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util])
  (:import [com.lmax.disruptor EventFactory EventHandler BlockingWaitStrategy RingBuffer
            WorkerPool IgnoreExceptionHandler WorkHandler
            InsufficientCapacityException]
           [com.lmax.disruptor.dsl Disruptor ProducerType]
           [java.util.concurrent Executors]
           [wonko_client.disruptor Event]))

(defn send-async [{:keys [queue] :as instance} topic message]
  (let [rb (.getRingBuffer (:disruptor queue))
        seq-num ((:next-fn queue) rb)]
    (if seq-num
      (try
        (let [ev (.get rb seq-num)]
          (.set ev {:instance instance :topic topic :message message})
          true)
        (finally
          (.publish rb seq-num)))
      false)))

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
        (kp/send instance topic message)))))

(defn make-worker-pool [work-handlers]
  (WorkerPool. (make-event-factory)
               (IgnoreExceptionHandler.)
               work-handlers))

(defn get-next-fn [drop-on-reject?]
  (if drop-on-reject?
    #(try
       (.tryNext %)
       (catch InsufficientCapacityException e
         (log/warn "Queue full. Dropping event.")))
    #(.next %)))

(defn init [{:keys [worker-count queue-size drop-on-reject?] :as options}]
  (let [executor (Executors/newCachedThreadPool)
        disruptor (make-disruptor queue-size executor)
        work-handlers (into-array (repeatedly worker-count make-work-handler))
        worker-pool (make-worker-pool work-handlers)
        rb (.start worker-pool executor)]
    (.handleEventsWithWorkerPool disruptor work-handlers)
    (.start disruptor)
    {:disruptor disruptor
     :worker-pool worker-pool
     :disruptor-executor executor
     :next-fn (get-next-fn drop-on-reject?)}))

(defn terminate [{:keys [queue] :as instance}]
  (let [{:keys [disruptor disruptor-executor]} queue]
    (.shutdown disruptor)
    (.shutdownNow disruptor-executor)))
