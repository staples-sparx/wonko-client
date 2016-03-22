(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util]
            [wonko-client.validation :as v]
            [clojure.tools.logging :as log]))

(defonce service
  (atom ""))

(defonce topics
  (atom {:events "wonko-events"
         :alerts "wonko-alerts"}))

(defonce thread-pool
  (atom nil))

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

(defn validate-and-send [message topic]
  (v/validate! message)
  (.submit @thread-pool #(kp/send message (get @topics topic))))

(defn counter [metric-name properties & {:as options}]
  (let [message (message metric-name properties nil options :counter)]
    (validate-and-send message :events)))

(defn gauge [metric-name properties metric-value & {:as options}]
  (let [message (message metric-name properties metric-value options :gauge)]
    (validate-and-send message :events)))

(defn stream [metric-name properties metric-value & {:as options}]
  (let [message (message metric-name properties metric-value options :stream)]
    (validate-and-send message :events)))

(defn alert [alert-name alert-info]
  (let [message (merge (message alert-name {} nil nil :counter)
                       {:alert-name alert-name
                        :alert-info alert-info})]
    (validate-and-send message :alerts)))

(defn set-topics! [events-topic alerts-topic]
  (reset! topics {:events events-topic
                  :alerts alerts-topic}))

(defn init! [service-name kafka-config & {:as options}]
  (let [validate? (or (:validate? options) false)
        thread-pool-size (or (:thread-pool-size options) 30)
        queue-size (or (:queue-size options) 30)]
    (reset! service service-name)
    (reset! thread-pool (util/create-fixed-threadpool thread-pool-size queue-size))
    (v/set-validation! validate?)
    (kp/init! kafka-config options)
    (log/info "wonko-client initialized for service" @service)
    nil))
