(ns wonko-client.kafka-producer
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp]
            [clojure.tools.logging :as log]
            [wonko-client.util :as util])
  (:import com.fasterxml.jackson.core.JsonGenerationException
           org.apache.kafka.common.serialization.Serializer
           org.apache.kafka.clients.producer.KafkaProducer
           [com.codahale.metrics MetricRegistry Timer]))

(def ^MetricRegistry metrics)
(def ^Timer serialize-timer)

(defn metrics-init []
  (alter-var-root #'metrics (constantly (MetricRegistry.)))
  (alter-var-root #'serialize-timer (constantly (util/create-timer metrics "serialize"))))

(deftype Jsonizer []
  Serializer
  (configure [_ _ _ ])
  (serialize [_ topic value]
    (util/with-time serialize-timer
      (.getBytes (json/generate-string value))))
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

(defn send [{:keys [producer exception-handler]} message topic]
  (try
    (let [record (kp/record topic message)]
      (kp/send producer record (partial callback exception-handler))
      true)
    (catch JsonGenerationException exception
      (exception-handler nil exception)
      false)))

(defn close [{:keys [^KafkaProducer producer]}]
  (.close producer))

(defn create [config options]
  (metrics-init)
  {:producer (create-producer config)
   :exception-handler (or (:exception-handler options)
                          default-exception-handler)})
