(ns wonko-client.kafka-producer-test
  (:require [wonko-client.kafka-producer :as wkp]
            [clojure.test :refer :all]
            [clj-kafka.new.producer :as kp]
            [wonko-client.test-util :as util]
            [wonko-client.test-util.kafka :as kafka]))

(deftest test-exception-handling
  (testing "the exception handler is not called when there are no exceptions"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)
      (let [producer (wkp/create util/kafka-config {:exception-handler exception-handler})]
        (is (wkp/send producer "message" topic-name))
        (is (empty? @exceptions))
        (kafka/delete-topic topic-name util/zookeeper))))

  (testing "the exception handler is called when there are exceptions"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)

      (let [producer (wkp/create util/kafka-config {:exception-handler exception-handler})]
        (wkp/close producer)
        (is (wkp/send producer "message" topic-name))
        (is (not (empty? @exceptions)))
        (is (re-find #"Failed to update metadata" (.getMessage (first @exceptions)))))

      (kafka/delete-topic topic-name util/zookeeper)))

  (testing "the exception handler is called when non-serializable data is passed in"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)

      (let [producer (wkp/create util/kafka-config {:exception-handler exception-handler})]
        (is (not (wkp/send producer java.lang.String topic-name)))
        (is (not (empty? @exceptions)))
        (is (re-find #"Cannot JSON encode" (.getMessage (first @exceptions)))))

      (kafka/delete-topic topic-name util/zookeeper)))

  (testing "the default exception handler is used if one isn't passed in"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [response exception]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)

      ;; This seems better than checking if the actual default handler
      ;; has printed to STDOUT, but suggestions welcome to remove this.
      (with-redefs [wkp/default-exception-handler exception-handler]
        (let [producer (wkp/create util/kafka-config {:exception-handler nil})]
          (wkp/close producer)
          (is (wkp/send producer "message" topic-name))
          (is (not (empty? @exceptions)))
          (is (re-find #"Failed to update metadata" (.getMessage (first @exceptions))))))

      (kafka/delete-topic topic-name util/zookeeper))))
