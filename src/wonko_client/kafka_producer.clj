(ns wonko-client.kafka-producer
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp])
  (:import [org.apache.kafka.clients.producer KafkaProducer]
           [org.apache.kafka.common.serialization Serializer]))

(deftype Jsonizer []
  Serializer
  (configure [_ _ _ ])
  (serialize [_ topic value]
    (.getBytes (json/generate-string value)))
  (close [_]))

(defn callback  [exception-handler message response exception]
  (when exception
    (exception-handler exception message response)))

(defn send-message [{:keys [producer exception-handler topics] :as instance} topic message]
  (try
    (let [record (kp/record (get topics topic) message)]
      (kp/send producer record (partial callback exception-handler message))
      true)
    (catch Exception e
      (exception-handler e message nil)
      false)))

(defn close [{:keys [^KafkaProducer producer]}]
  (.close producer))

(defn create [config]
  (kp/producer config
               (kp/string-serializer)
               (Jsonizer.)))
