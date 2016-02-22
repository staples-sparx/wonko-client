(ns wonko-client.kafka-producer
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp])
  (:import [org.apache.kafka.common.serialization Serializer]
           [org.apache.kafka.clients.producer Producer]
           [com.fasterxml.jackson.core JsonGenerationException]))

(defonce ^Producer producer
  (atom nil))

(def ^String topic
  "wonko-events")

(deftype Jsonizer []
  Serializer
  (configure [_ _ _ ])
  (serialize [_ topic value]
    (.getBytes (json/generate-string value)))
  (close [_]))

(defn create-producer [config]
  (kp/producer config
               (kp/string-serializer)
               (Jsonizer.)))

(defn send-message [message]
  (try
   (let [record (kp/record topic message)]
     @(kp/send @producer record)
     true)
   (catch JsonGenerationException e
     ;; message not sent
     false)))

(defn init! [config]
  (reset! producer (create-producer config))
  nil)
