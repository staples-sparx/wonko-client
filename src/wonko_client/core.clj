(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]))

(defonce ^String service
  (atom ""))

(defn counter [metric-name properties & {:as options}]
  (kp/send-message {:service @service
                    :metric-type :counter
                    :metric-name metric-name
                    :properties properties
                    :options options}))

(defn gauge [metric-name properties metric-value & {:as options}]
  (kp/send-message {:service @service
                    :metric-type :gauge
                    :metric-name metric-name
                    :properties properties
                    :metric-value metric-value
                    :options options}))

(defn init! [service-name kafka-config]
  (reset! service service-name)
  (kp/init! kafka-config))
