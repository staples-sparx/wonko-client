(ns wonko-client.core-test
  (:require [cheshire.core :as json]
            [clj-kafka.consumer.zk :as kc]
            [clj-kafka.core :as k]
            [clojure.test :refer :all]
            [wonko-client.core :as core]
            [wonko-client.util :as u]
            [wonko-client.test-util :as util]))

(defn consume [topic n]
  (k/with-resource [c (kc/consumer util/zookeeper-config)]
    kc/shutdown
    (doall (->> (kc/messages c topic)
                (take n)
                (map :value)
                (map #(String. %))
                (map #(json/decode % true))))))

(deftest test-basic-message-sending
  (testing "counters, gauges, streams and alerts work correctly"
    (let [topics (util/create-test-topics)]
      (core/init! "test-service"
                  util/kafka-config
                  :validate? true
                  :topics topics)

      (core/counter :test-counter {:some :prop})
      (core/gauge :test-gauge {:some :prop} 6)
      (core/stream :test-stream {:some :prop} 10)
      (core/alert :test-alert {:arbitrary :info})

      (let [messages (->> (consume (:events topics) 3)
                          (group-by :metric-type))]
        (is (= {:metric-name "test-counter"
                :metric-type "counter"
                :metric-value nil
                :properties {:some "prop"}
                :service "test-service"}
               (select-keys (first (get messages "counter"))
                            [:metric-name :metric-type :metric-value :properties :service])))

        (is (= {:metric-name "test-gauge"
                :metric-type "gauge"
                :metric-value 6
                :properties {:some "prop"}
                :service "test-service"}
               (select-keys (first (get messages "gauge"))
                            [:metric-name :metric-type :metric-value :properties :service])))

        (is (= {:metric-name "test-stream"
                :metric-type "stream"
                :metric-value 10
                :properties {:some "prop"}
                :service "test-service"}
               (select-keys (first (get messages "stream"))
                            [:metric-name :metric-type :metric-value :properties :service]))))

      (let [message (first (consume (:alerts topics) 1))]
        (is (= {:metric-name "test-alert"
                :metric-type "counter"
                :metric-value nil
                :properties {}
                :service "test-service"
                :alert-name "test-alert"
                :alert-info {:arbitrary "info"}}
               (select-keys message
                            [:metric-name :metric-type :metric-value :properties :service
                             :alert-name :alert-info]))))

      (util/delete-topics (:events topics) (:alerts topics)))))

(deftest test-options
  (testing "`validate? true` turns on validation"
    (core/init! "test-service" util/kafka-config :validate? true)
    (is (thrown?
         clojure.lang.ExceptionInfo
         (core/counter 123 {:some :prop})))

    (core/init! "test-service" util/kafka-config :validate? false)
    (is (core/counter 123 {:some :prop})))

  (testing "queue configs"
    (let [worker-count 6
          queue-size 6]
      (core/init! "test-service" util/kafka-config :worker-count worker-count :queue-size queue-size)
      (let [actual-worker-count (count (:workerSequences (bean (:worker-pool (:queue core/instance)))))
            actual-queue-size (:bufferSize (bean (:disruptor (:queue core/instance))))]
        (is (= (inc worker-count) actual-worker-count))
        (is (= (u/round-up-to-power-of-2 queue-size) actual-queue-size))))))

(deftest test-alerts-are-synchronous
  (testing "alerts are not affected by the threadpool, they are synchronous"
    (let [topics (util/create-test-topics)]
      (core/init! "test-service"
                  util/kafka-config
                  :worker-count 1
                  :queue-size 1
                  :topics topics
                  :drop-on-reject? true)

      (core/q-terminate core/instance)
      (core/alert :test-alert-name {:alert :info})

      (is (= 1 (count (consume (:alerts topics) 1))))
      (util/delete-topics (:events topics) (:alerts topics)))))
