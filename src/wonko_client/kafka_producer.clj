(ns wonko-client.kafka-producer
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp])
  (:import [org.apache.kafka.common.serialization Serializer]
           [org.apache.kafka.clients.producer Producer]
           [com.fasterxml.jackson.core JsonGenerationException]))

(defonce producer
  (atom nil))

(defonce exception-handler
  (atom nil))

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

(defn default-exception-handler [response ex]
  (prn {:msg "Unable to send message to kafka."
        :response response
        :ex (bean ex)}))

(defn send-callback [response ex]
  (when ex
    (exception-handler response ex)))

(defn send-message [message topic]
  (try
    (let [record (kp/record topic message)]
      (kp/send @producer record exception-handler)
      true)
    (catch JsonGenerationException e
      ;; message not sent
      false)))

(defn init! [config options]
  (reset! producer (create-producer config))
  (reset! exception-handler (or (:exception-handler options)
                                default-exception-handler))
  nil)
