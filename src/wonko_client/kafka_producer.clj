(ns wonko-client.kafka-producer
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp]
            [clojure.tools.logging :as log])
  (:import com.fasterxml.jackson.core.JsonGenerationException
           [org.apache.kafka.clients.producer KafkaProducer BufferExhaustedException]
           org.apache.kafka.common.serialization.Serializer))

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
  (log/error ex {:msg "Unable to send message to kafka."
                 :response response
                 :ex (bean ex)}))

(defn callback  [exception-handler response exception]
  (when exception
    (exception-handler response exception)))

(defn send [{:keys [topics] :as instance} topic message]
  (let [{:keys [producer exception-handler]} (:producer instance)]
    (try
      (let [record (kp/record (get topics topic) message)]
        (kp/send producer record (partial callback exception-handler))
        true)
      (catch Exception e
        (exception-handler nil e)
        false))))

(defn close [{:keys [^KafkaProducer producer]}]
  (.close producer))

(defn create [config options]
  {:producer (create-producer config)
   :exception-handler (or (:exception-handler options)
                          default-exception-handler)})
