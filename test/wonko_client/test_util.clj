(ns wonko-client.test-util
  (:require [wonko-client.test-util.kafka :as kafka]))

(def kafka-config
  {"bootstrap.servers" "127.0.0.1:9092"
   "compression.type" "gzip"
   "metadata.fetch.timeout.ms" 500
   "linger.ms" 5})

(def zookeeper-config
  {"zookeeper.connect" "localhost:2182"
   "group.id" "clj-kafka.consumer"
   "auto.offset.reset" "smallest"
   "auto.commit.enable" "true"
   "auto.commit.interval.ms" "1000"})

(def zookeeper "localhost:2182")

(defn rand-str [prefix]
  (->> (.nextInt (java.util.concurrent.ThreadLocalRandom/current) 0 999999)
       (str prefix "-")))

(defn create-test-topics []
  (let [events-topic (rand-str "wonko-events")
        alerts-topic (rand-str "wonko-alerts")]
    (kafka/create-topic events-topic zookeeper)
    (kafka/create-topic alerts-topic zookeeper)
    {:events events-topic
     :alerts alerts-topic}))

(defn delete-topics [events-topic alerts-topic]
  (kafka/delete-topic events-topic zookeeper)
  (kafka/delete-topic alerts-topic zookeeper))
