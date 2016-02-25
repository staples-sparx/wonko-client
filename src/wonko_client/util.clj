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

(defn hostname
  "Returns the current host name"
  []
  (.getHostName (java.net.InetAddress/getLocalHost)))

(defn ip-address
  "Returns the string representation of the IP of the local host"
  []
  (.getHostAddress (java.net.InetAddress/getLocalHost)))

(defn current-timestamp []
  (System/currentTimeMillis))
