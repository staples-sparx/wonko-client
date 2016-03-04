(ns wonko-client.kafka-producer-test
  (:require [wonko-client.kafka-producer :as sut]
            [clojure.test :refer :all]
            [clj-kafka.new.producer :as kp]
            [wonko-client.test-util :as util]
            [wonko-client.test-util.kafka :as kafka]))

(def kafka-config
  {"bootstrap.servers" "127.0.0.1:9092"
   "compression.type" "gzip"
   "metadata.fetch.timeout.ms" 500
   "linger.ms" 5})

(def zookeeper "localhost:2182")

(deftest test-exception-handling
  (testing "the exception handler is not called when there are no exceptions"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name zookeeper)

      (sut/init! kafka-config {:exception-handler exception-handler})
      (is (sut/send "message" topic-name))
      (is (empty? @exceptions))

      (kafka/delete-topic topic-name zookeeper)))

  (testing "the exception handler is called when there are exceptions"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name zookeeper)

      (sut/init! kafka-config {:exception-handler exception-handler})
      (.close @sut/producer)
      (is (sut/send "message" topic-name))
      (is (not (empty? @exceptions)))
      (is (re-find #"Failed to update metadata" (.getMessage (first @exceptions))))

      (kafka/delete-topic topic-name zookeeper)))

  (testing "the exception handler is called when non-serializable data is passed in"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name zookeeper)

      (sut/init! kafka-config {:exception-handler exception-handler})
      (is (not (sut/send java.lang.String topic-name)))
      (is (not (empty? @exceptions)))
      (is (re-find #"Cannot JSON encode" (.getMessage (first @exceptions))))

      (kafka/delete-topic topic-name zookeeper)))

  (testing "the default exception handler is used if one isn't passed in"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name zookeeper)

      ;; This seems better than checking if the actual default handler
      ;; has printed to STDOUT, but suggestions welcome to remove this.
      (with-redefs [sut/default-exception-handler exception-handler]
        (sut/init! kafka-config {:exception-handler nil})
        (.close @sut/producer)
        (is (sut/send "message" topic-name))
        (is (not (empty? @exceptions)))
        (is (re-find #"Failed to update metadata" (.getMessage (first @exceptions)))))

      (kafka/delete-topic topic-name zookeeper))))
