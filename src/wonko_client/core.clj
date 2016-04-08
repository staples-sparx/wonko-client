(ns wonko-client.core
  (:require [wonko-client.kafka-producer :as kp]
            [wonko-client.util :as util]
            [wonko-client.validation :as v]
            [clojure.tools.logging :as log]))

(def ^:private default-topics
  {:events "wonko-events"
   :alerts "wonko-alerts"})

(defonce ^:private instance
  (atom nil))

(def ^:private default-metadata
  {:host (util/hostname)
   :ip-address (util/ip-address)})

(defn- metadata []
  (assoc default-metadata
    :ts (util/current-timestamp)))

(defn- message [{:keys [service]} metric-name properties metric-value options metric-type]
  {:service      service
   :metadata     (metadata)
   :metric-name  metric-name
   :metric-type  metric-type
   :metric-value metric-value
   :properties   properties
   :options      options})

(defn- validate-and-send [{:keys [thread-pool topics producer]} message topic]
  (v/validate! message)
  (.submit thread-pool #(kp/send producer message (get topics topic))))

(defn counter [metric-name properties & {:as options}]
  (when-let [this @instance]
    (let [message (message this metric-name properties nil options :counter)]
    (validate-and-send this message :events))))

(defn gauge [metric-name properties metric-value & {:as options}]
  (when-let [this @instance]
    (let [message (message this metric-name properties metric-value options :gauge)]
      (validate-and-send this message :events))))

(defn stream [metric-name properties metric-value & {:as options}]
  (when-let [this @instance]
    (let [message (message this metric-name properties metric-value options :stream)]
      (validate-and-send this message :events))))

(defn alert [alert-name alert-info]
  (when-let [this @instance]
    (let [message (merge (message this alert-name {} nil nil :counter)
                         {:alert-name alert-name
                          :alert-info alert-info})]
      (validate-and-send this message :alerts))))

(defn set-topics! [events-topic alerts-topic]
  (swap! instance assoc :topics {:events events-topic
                                 :alerts alerts-topic}))

(defn init! [service-name kafka-config & {:as options}]
  (let [validate?        (or (:validate? options) false)
        thread-pool-size (or (:thread-pool-size options) 10)
        queue-size       (or (:queue-size options) 10)]
    (reset! instance
            {:service service-name
             :topics default-topics
             :thread-pool (util/create-fixed-threadpool thread-pool-size queue-size)
             :producer (kp/create kafka-config options)})
    (v/set-validation! validate?)
    (log/info "wonko-client initialized for service" service-name)
    nil))
