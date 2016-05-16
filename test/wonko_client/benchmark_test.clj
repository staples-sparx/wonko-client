(ns wonko-client.benchmark-test
  (:require [wonko-client.core :as wonko]
            [wonko-client.collectors :as wc]
            [wonko-client.util]
            [throttler.core :as throttler]
            [clojure.test :refer :all]))

(def kafka-config
  {"bootstrap.servers" "localhost:9092",
   "reconnect.backoff.ms" 50,
   "request.timeout.ms" 2,
   "retry.backoff.ms" 10,
   "linger.ms" 5,
   "timeout.ms" "10",
   "total.memory.bytes" (* 1024 1024 120),
   "metadata.fetch.timeout.ms" 10,
   "block.on.buffer.full" "false",
   "queue.enqueue.timeout.ms" 0,
   "compression.type" "gzip"})

(defn init! []
  (wonko/init! "test"
               kafka-config
               :thread-pool-size 10
               :queue-size 10))

(defn low-latency-service
  "Run this at 1000 per second"
  []
  (wonko/stream :some-api-call {:status 200} 999999999)
  (wonko/counter :something-less-important-1 {:this :might :have :some :properties :too})
  (wonko/counter :something-less-important-2 {:this :might :have :some :properties :too})
  (wonko/gauge :something-less-important-1 {:this :might :have :some :properties :too} 140M)
  (wonko/gauge :something-less-important-2 {:this :might :have :some :properties :too} 34.39))

(defn collector
  "Run this in intervals of 5s"
  []
  (dotimes [_ 2500]
    (wonko/gauge :collector-metric {:this :might :have :some :properties :too} 9999999999)))

(defn run [total-requests request-rate]
  (init!)
  (let [throttled-fn (throttler/throttle-fn low-latency-service request-rate :second (* 2 request-rate))
        start-time (System/currentTimeMillis)]
    (doall (pmap (fn [_] (throttled-fn)) (range total-requests)))
    (prn {:total-requests total-requests
          :request-rate request-rate
          :exec-time-ms (- (System/currentTimeMillis) start-time)}))
  (wonko/terminate!))
