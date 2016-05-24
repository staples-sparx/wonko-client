(ns wonko-client.benchmark-test
  (:require [wonko-client.core :as wonko]
            [wonko-client.collectors :as wc]
            [wonko-client.util :as util]
            [throttler.core :as throttler]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent TimeUnit]))

(def kafka-config
  {"bootstrap.servers" "localhost:9092",
   "reconnect.backoff.ms" 50,
   "request.timeout.ms" 2,
   "retry.backoff.ms" 10,
   "linger.ms" 5,
   "timeout.ms" "10",
   "total.memory.bytes" (* 1024 1024 120),
   "metadata.fetch.timeout.ms" 10,
   "block.on.buffer.full" "true",
   "queue.enqueue.timeout.ms" 0,
   "compression.type" "gzip"})

(def correct-kafka-config
  {"bootstrap.servers" "localhost:9092",
   "reconnect.backoff.ms" 50,
   "request.timeout.ms" 2,
   "retry.backoff.ms" 10,
   "linger.ms" 5,
   "timeout.ms" "10",
   "total.memory.bytes" (* 1024 1024 120),
   "metadata.fetch.timeout.ms" 10,
   "block.on.buffer.full" "true",
   "queue.enqueue.timeout.ms" 0,
   "compression.type" "gzip"})

;; "reconnect.backoff.ms" 50,
;; "request.timeout.ms" 2,
;; "retry.backoff.ms" 10,
;; "metadata.fetch.timeout.ms" 10
;; "queue.enqueue.timeout.ms" 0

(def without-timeouts-kafka-config
  {"bootstrap.servers" "localhost:9092",
   "linger.ms" 5,
   "total.memory.bytes" (* 1024 1024 120),
   "block.on.buffer.full" "true",
   "compression.type" "gzip"})

(defn init! []
  (wonko/terminate!)
  (wonko/init! "test"
               without-timeouts-kafka-config
               :thread-pool-size 10
               :queue-size 100
               :drop-on-reject? false))

(defn mock-service
  "Run this at 1000 per second"
  [integrate? latency-ms]
  (Thread/sleep latency-ms)
  (log/debug :mock-service)
  (when integrate?
    (wonko/stream :some-api-call {:status 200} 999999999)
    (wonko/counter :something-less-important-1 {:this :might :have :some :properties :too})
    (wonko/counter :something-less-important-2 {:this :might :have :some :properties :too})
    (wonko/gauge :something-less-important-1 {:this :might :have :some :properties :too} 140M)
    (wonko/gauge :something-less-important-2 {:this :might :have :some :properties :too} 34.39)))

(defn collector
  "Run this in intervals of 5s"
  [integrate? interval-ms metrics-count]
  (when integrate?
    (Thread/sleep interval-ms)
    (dotimes [n metrics-count]
      (wonko/gauge (str "collector-metric-" n) {:this :might :have :some :properties :too} 9999999999))
    (recur integrate? interval-ms metrics-count)))

(defn run [integrate? {:keys [service-latency-ms
                              total-requests
                              request-rate
                              collector-interval-ms
                              collector-metrics-count]}]
  (init!)
  (let [throttled-fn (throttler/throttle-fn #(mock-service integrate? service-latency-ms)
                                            request-rate :second (* 2 request-rate))
        tp (util/create-fixed-threadpool {:thread-pool-size 200 :queue-size 1000 :drop-on-reject? false})
        start-time (System/currentTimeMillis)
        collector-f (future (collector integrate? collector-interval-ms collector-metrics-count))]
    (dotimes [_ total-requests]
      (.submit tp throttled-fn))
    (future-cancel collector-f)
    (prn {:total-requests total-requests
          :request-rate request-rate
          :exec-time-ms (- (System/currentTimeMillis) start-time)})
    (wonko/terminate!)
    (.shutdown tp)
    (.awaitTermination tp 10 TimeUnit/SECONDS)))
