(ns wonko-client.host-metrics
  (:require [wonko-client.core :as client]
            [wonko-client.host-metrics.collect :as collect]
            [wonko-client.util :as util]
            [clojure.tools.logging :as log]))

(defonce daemon (atom nil))

(defn send-wonko-events []
  (try
    (doseq [{:keys [metric-name properties metric-value] :as event} (collect/events)]
      (client/gauge metric-name properties metric-value))
    (catch Exception e
      (log/error e "Unable to send host metrics."))))

(defn start
  ([]
     (start 5000))
  ([sleep-ms]
     (reset! daemon (util/start-daemon send-wonko-events sleep-ms))
     (log/info "Started collecting host metrics with an interval of" sleep-ms)
     nil))

(defn stop []
  (util/stop-daemon @daemon)
  (swap! daemon (constantly nil)))
