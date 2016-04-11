(ns wonko-client.util
  (:import [java.util.concurrent
            ThreadPoolExecutor
            TimeUnit
            ArrayBlockingQueue
            ThreadPoolExecutor$CallerRunsPolicy
            ThreadPoolExecutor$DiscardPolicy]))

(defn start-daemon [f sleep-ms]
  (doto (Thread. (fn []
                   (f)
                   (Thread/sleep sleep-ms)
                   (recur)))
    (.setDaemon true)
    (.start)))

(defn stop-daemon [daemon]
  (.interrupt ^Thread daemon))

(defn create-fixed-threadpool [{:keys [thread-pool-size queue-size drop-on-reject?]}]
  (ThreadPoolExecutor. thread-pool-size
                       thread-pool-size
                       60
                       TimeUnit/SECONDS
                       (ArrayBlockingQueue. queue-size true)
                       (if drop-on-reject?
                         (ThreadPoolExecutor$DiscardPolicy.)
                         (ThreadPoolExecutor$CallerRunsPolicy.))))
