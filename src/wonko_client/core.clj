(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util]))

(defonce service
  (atom ""))

(defonce topics
  (atom {:events "wonko-events"
         :alerts "wonko-alerts"}))

(defn metadata []
  {:host (util/hostname)
   :ip-address (util/ip-address)
   :ts (util/current-timestamp)})

(defn counter [metric-name properties & {:as options}]
  (kp/send-message {:service @service
                    :metadata (metadata)
                    :metric-type :counter
                    :metric-name metric-name
                    :properties properties
                    :options options}
                   (:events @topics)))

(defn gauge [metric-name properties metric-value & {:as options}]
  (kp/send-message {:service @service
                    :metadata (metadata)
                    :metric-type :gauge
                    :metric-name metric-name
                    :properties properties
                    :metric-value metric-value
                    :options options}
                   (:events @topics)))

(defn alert [alert-name alert-info]
  (kp/send-message {:service @service
                    :metadata (metadata)
                    :alert-name alert-name
                    :alert-info alert-info
                    :metric-name alert-name
                    :properties {}
                    :metric-type :counter}
                   (:alerts @topics)))

(defn set-topics! [events-topic alerts-topic]
  (reset! topics {:events events-topic
                  :alerts alerts-topic}))

(defn init! [service-name kafka-config & {:as options}]
  (reset! service service-name)
  (kp/init! kafka-config options))
