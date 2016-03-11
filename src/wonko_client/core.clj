(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util]
            [wonko-client.validation :as v]))

(defonce service
  (atom ""))

(defonce topics
  (atom {:events "wonko-events"
         :alerts "wonko-alerts"}))

(defn metadata []
  {:host (util/hostname)
   :ip-address (util/ip-address)
   :ts (util/current-timestamp)})

(defn message [metric-name properties metric-value options metric-type]
  {:service      @service
   :metadata     (metadata)
   :metric-name  metric-name
   :metric-type  metric-type
   :metric-value metric-value
   :properties   properties
   :options      options})

(defn counter [metric-name properties & {:as options}]
  (let [message (message metric-name properties nil options :counter)]
    (v/validate! message)
    (kp/send message (:events @topics))))

(defn gauge [metric-name properties metric-value & {:as options}]
  (let [message (message metric-name properties metric-value options :gauge)]
    (v/validate! message)
    (kp/send message (:events @topics))))

(defn stream [metric-name properties metric-value & {:as options}]
  (let [message (message metric-name properties metric-value options :stream)]
    (v/validate! message)
    (kp/send message (:events @topics))))

(defn alert [alert-name alert-info]
  (let [message (merge (message alert-name {} nil nil :counter)
                       {:alert-name alert-name
                        :alert-info alert-info})]
    (kp/send message (:alerts @topics))))

(defn set-topics! [events-topic alerts-topic]
  (reset! topics {:events events-topic
                  :alerts alerts-topic}))

(defn init! [service-name kafka-config & {:as options}]
  (reset! service service-name)
  (v/set-validation! (or (:validate? options) false))
  (kp/init! kafka-config options))
