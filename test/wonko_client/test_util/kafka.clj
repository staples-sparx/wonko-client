(ns wonko-client.test-util.kafka
  (require [clj-kafka.admin :as admin]
           [clj-kafka.zk :as zk]))

(defn create-topic [topic zookeeper]
  (with-open [zk (admin/zk-client zookeeper)]
    (if-not (admin/topic-exists? zk topic)
      (admin/create-topic zk
                          topic
                          {:partitions 1
                           :replication-factor 1
                           :config {"cleanup.policy" "compact"}}))))

(defn delete-topic [topic zookeeper]
  (with-open [zk (admin/zk-client zookeeper)]
    (admin/delete-topic zk topic)))

(defn list-topics [zookeeper]
  (zk/topics {"zookeeper.connect" zookeeper}))

(defn delete-all-topics [zookeeper]
  (for [topic (list-topics)]
    (delete-topic topic zookeeper)))
