(ns wonko-client.core-test
  (:require [cheshire.core :as json]
            [clj-kafka.consumer.zk :as kc]
            [clj-kafka.core :as k]
            [clojure.test :refer :all]
            [wonko-client.core :as core]
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

  (testing "thread pool configs"
    (core/init! "test-service" util/kafka-config :thread-pool-size 6 :queue-size 6)
    (is (= 6 (.getCorePoolSize (:thread-pool core/instance))))
    (is (= 6 (.getMaximumPoolSize (:thread-pool core/instance))))
    (is (= 6 (.remainingCapacity (.getQueue (:thread-pool core/instance)))))))

(deftest test-alerts-are-synchronous
  (testing "alerts are not affected by the threadpool, they are synchronous"
    (let [topics (util/create-test-topics)
          time-taking-task #(Thread/sleep 1000)]
      (core/init! "test-service"
                  util/kafka-config
                  :thread-pool-size 1
                  :queue-size 1
                  :topics topics
                  :drop-on-reject? true)

      (.submit (:thread-pool core/instance) time-taking-task) ;; pool full
      (.submit (:thread-pool core/instance) time-taking-task) ;; queue full
      (core/alert :test-alert-name {:alert :info})

      (is (= 1 (count (consume (:alerts topics) 1))))
      (is (= 0 (.remainingCapacity (.getQueue (:thread-pool core/instance)))))

      (util/delete-topics (:events topics) (:alerts topics)))))
