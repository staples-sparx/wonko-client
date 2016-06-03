(ns wonko-client.util
  (:require [clojure.tools.logging :as log])
  (:import [java.util.concurrent ThreadPoolExecutor Executors TimeUnit]))

(defn create-scheduled-tp [f rate]
  (doto (Executors/newScheduledThreadPool 1 )
    (.scheduleWithFixedDelay f 0 rate TimeUnit/MILLISECONDS)))

(defn stop-tp [^ThreadPoolExecutor tp]
  (when tp
    (.shutdownNow tp)))

(defn round-up-to-power-of-2 [x]
  (let [exp (- 32  (Integer/numberOfLeadingZeros (dec (int x))))]
    (int (Math/pow 2 exp))))
