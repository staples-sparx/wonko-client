(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]))

(defonce ^String service
  (atom ""))

(defn counter [metric-name properties & {:as options}]
  (kp/send-message {:service @service
                    :metric-type :counter
                    :metric-name metric-name
                    :properties properties
                    :options options}
                   "wonko-events"))

(defn gauge [metric-name properties metric-value & {:as options}]
  (kp/send-message {:service @service
                    :metric-type :gauge
                    :metric-name metric-name
                    :properties properties
                    :metric-value metric-value
                    :options options}
                   "wonko-events"))

(defn alert [alert-name alert-info]
  (kp/send-message {:service @service
                    :alert-name alert-name
                    :alert-info alert-info
                    :metric-type :counter
                    :metric-name alert-name
                    :properties {}}
                   "wonko-alerts"))

(defn init! [service-name kafka-config]
  (reset! service service-name)
  (kp/init! kafka-config))
