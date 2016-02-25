(ns wonko-client.host-metrics
  (:require [wonko-client.core :as client]
            [wonko-client.host-metrics.collect :as collect]
            [wonko-client.util :as util]))

(defonce daemon (atom nil))

(defn send-wonko-events []
  (doseq [event (collect/events)]
    (apply client/gauge event)))

(defn start
  ([]
     (start 5000))
  ([sleep-ms]
     (reset! daemon (util/start-daemon send-wonko-events sleep-ms))))

(defn stop []
  (util/stop-daemon @daemon)
  (swap! daemon (constantly nil)))
