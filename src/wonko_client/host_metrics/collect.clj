(ns wonko-client.host-metrics.collect
  (:require [clojure.java.jmx :as jmx]
            [clojure.string :as s]))

(defn mbean-names []
  (map #(str %) (jmx/mbean-names "*:*")))

(defn threading []
  (select-keys (jmx/mbean "java.lang:type=Threading")
               [:ThreadCount
                :PeakThreadCount
                :DaemonThreadCount
                :CurrentThreadCpuTime
                :TotalStartedThreadCount
                :CurrentThreadUserTime]))

(defn cpu []
  (select-keys (jmx/mbean "java.lang:type=OperatingSystem")
               [:ProcessCpuLoad
                :OpenFileDescriptorCount
                :TotalSwapSpaceSize
                :CommittedVirtualMemorySize
                :ProcessCpuTime
                :SystemCpuLoad
                :SystemLoadAverage
                :FreePhysicalMemorySize
                :TotalPhysicalMemorySize
                :FreeSwapSpaceSize
                :AvailableProcessors]))

(defn memory []
  (select-keys (jmx/mbean "java.lang:type=Memory")
               [:HeapMemoryUsage :NonHeapMemoryUsage]))

(defn memory-pools []
  (into {}
        (for [mbean-name (filter #(re-find #"MemoryPool" %) (mbean-names))
              :let [metrics (jmx/mbean mbean-name)]]
          {(:Name metrics) (select-keys metrics [:PeakUsage :CollectionUsage :Usage])})))

(defn garbage-collection []
  (->> (filter #(re-find #"Garbage" %) (mbean-names))
       (map (fn [mbean-name]
              (let [metrics (jmx/mbean mbean-name)]
                {(:Name metrics)
                 {:LastGcInfo
                  {:GcThreadCount (get-in metrics [:LastGcInfo :GcThreadCount])
                   :duration  (get-in metrics [:LastGcInfo :duration])}
                  :CollectionCount (:CollectionCount metrics)
                  :CollectionTime (:CollectionTime metrics)}})))
       (into {})))

(defn wonko-name
  ([metric-class metric-name]
     (s/join "-" [(name metric-class) (name metric-name)]))
  ([prefix metric-class metric-name]
     (s/join "-" [prefix (name metric-class) (name metric-name)])))

(defn jmx->wonko [metric-class metrics]
  (mapcat (fn [[metric-name metric-value]]
            (if (map? metric-value)
              (jmx->wonko (wonko-name metric-class metric-name) metric-value)
              [[(wonko-name "jvm" metric-class metric-name) nil metric-value]]))
          metrics))
