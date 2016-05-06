(ns wonko-client.collectors
  (:require [wonko-client.collectors.host-metrics :as hm]
            [wonko-client.collectors.ping :as ping]
            [wonko-client.util :as util]
            [clojure.tools.logging :as log]))

(defonce collectors
  (atom
   {:host-metrics {:f hm/send-metrics :sleep-ms 5000 :log-msg "Sending host metrics." :daemon nil}
    :ping         {:f ping/send-ping :sleep-ms 5000 :log-msg "Sending ping." :daemon nil}
    :postgresql   {:f nil :sleep-ms 5000 :log-msg "Sending postgresql metrics." :daemon nil}}))

(defn ->collector-fn [collector-name {:keys [f sleep-ms log-msg]}]
  (fn []
    (try
      (log/info log-msg)
      (f)
      (catch Exception e
        (log/error e "Unable to collect metrics using collector" collector-name)))
    (Thread/sleep sleep-ms)
    (recur)))

(defn start-collector [collector-name collector-info]
  (swap! collectors assoc-in [collector-name :daemon]
         (util/start-daemon (->collector-fn collector-name collector-info))))

(defn stop-collector [collector-name {:keys [daemon] :as collector-info}]
  (log/info "Stopping collector" collector-name)
  (util/stop-daemon daemon)
  (swap! collectors update collector-name dissoc :daemon))

(defn stop [& collector-names]
  (doseq [collector-name collector-names]
    (stop-collector collector-name (get @collectors collector-name))))

(defn conditionally-include-collectors [collector-names]
  (when (contains? (set collector-names) :postgresql)
    (require 'wonko-client.collectors.postgresql)
    (let [f (ns-resolve (find-ns 'wonko-client.collectors.postgresql) 'send-metrics)]
      (swap! collectors assoc-in [:postgresql :f] f))))

(defn start [& collector-names]
  (conditionally-include-collectors collector-names)
  (doseq [collector-name collector-names]
    (start-collector collector-name (get @collectors collector-name))))
