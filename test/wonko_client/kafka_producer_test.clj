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
          exception-handler (fn [exception message response]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)
      (let [producer (wkp/create util/kafka-config)
            instance {:producer producer
                      :topics {:events topic-name}
                      :exception-handler exception-handler}]
        (is (wkp/send instance :events "message"))
        (is (empty? @exceptions))
        (kafka/delete-topic topic-name util/zookeeper))))

  (testing "the exception handler is called when there are exceptions"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [exception message response]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)

      (let [producer (wkp/create util/kafka-config)
            instance {:producer producer
                      :topics {:events topic-name}
                      :exception-handler exception-handler}]
        (wkp/close instance)
        (is (wkp/send instance :events "message"))
        (is (not (empty? @exceptions)))
        (is (re-find #"Failed to update metadata" (.getMessage (first @exceptions)))))

      (kafka/delete-topic topic-name util/zookeeper)))

  (testing "the exception handler is called when non-serializable data is passed in"
    (let [topic-name        (util/rand-str "test-topic")
          exceptions        (atom [])
          exception-handler (fn [exception message response]
                              (swap! exceptions conj exception))]
      (kafka/create-topic topic-name util/zookeeper)

      (let [producer (wkp/create util/kafka-config)
            instance {:producer producer
                      :topics {:events topic-name}
                      :exception-handler exception-handler}]
        (is (not (wkp/send instance :events java.lang.String)))
        (is (not (empty? @exceptions)))
        (is (re-find #"Cannot JSON encode" (.getMessage (first @exceptions)))))

      (kafka/delete-topic topic-name util/zookeeper))))
