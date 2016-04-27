(ns wonko-client.message
  (:require [wonko-client.message.validation :as v]))

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(def ip-address
  (.getHostAddress (java.net.InetAddress/getLocalHost)))

(defn- metadata []
  {:host hostname
   :ip-address ip-address
   :ts (System/currentTimeMillis)})

(defn- build* [service metric-name properties metric-value options metric-type]
  {:service      service
   :metadata     (metadata)
   :metric-name  metric-name
   :metric-type  metric-type
   :metric-value metric-value
   :properties   properties
   :options      options})

(defn build [service metric-name properties metric-value options metric-type]
  (let [m (build* service metric-name properties metric-value options metric-type)]
    (v/validate! m)
    m))

(defn build-alert [service metric-name properties metric-value
                   alert-name alert-info options metric-type]
  (let [m (assoc (build* service metric-name properties metric-value options metric-type)
            :alert-name alert-name
            :alert-info alert-info)]
    (v/validate! m)
    m))
