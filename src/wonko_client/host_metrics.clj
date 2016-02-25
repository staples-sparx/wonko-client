(ns wonko-client.host-metrics
  (:require [wonko-client.core :as client]
            [wonko-client.host-metrics.collect :as collect]
            [wonko-client.util :as util]))

(defonce daemon (atom nil))

(defn get-events []
  (mapcat (fn [[metric-class metrics]]
            (collect/jmx->wonko metric-class metrics))
          {:memory (collect/memory)
           :cpu (collect/cpu)
           :threading (collect/threading)
           :garbage-collection (collect/garbage-collection)
           :memory-pools (collect/memory-pools)}))

(defn send-wonko-events []
  (doseq [event (get-events)]
    (apply client/gauge event)))

(defn start
  ([]
     (start 5000))
  ([sleep-ms]
     (reset! daemon (util/start-daemon send-wonko-events sleep-ms))))

(defn stop []
  (util/stop-daemon @daemon)
  (swap! daemon (constantly nil)))
