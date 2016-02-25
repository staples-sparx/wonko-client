(ns wonko-client.util)

(defn start-daemon [f sleep-ms]
  (doto (Thread. (fn []
                   (f)
                   (Thread/sleep sleep-ms)
                   (recur)))
    (.setDaemon true)
    (.start)))

(defn stop-daemon [daemon]
  (.interrupt ^Thread daemon))
