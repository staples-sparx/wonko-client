(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]))

(defn counter [metric-name properties & {:as options}]
  (kp/send-message {:metric-type :counter
                    :metric-name metric-name
                    :properties properties
                    :options options}))

(defn gauge [metric-name properties metric-value & {:as options}]
  (kp/send-message {:metric-type :gauge
                    :metric-name metric-name
                    :properties properties
                    :metric-value metric-value
                    :options options}))

(defn init! [config]
  (kp/init! config))
