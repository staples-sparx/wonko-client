(ns wonko-client.collectors
  (:require [wonko-client.collectors.host-metrics :as hm]
            [wonko-client.collectors.ping :as ping]
            [wonko-client.util :as util]
            [clojure.tools.logging :as log]))

(defonce collectors
  (atom
   {:host-metrics {:f hm/send-metrics :rate-ms 5000 :tp nil}
    :ping         {:f ping/send-ping :rate-ms 5000 :tp nil}
    :postgresql   {:f nil :rate-ms 60000 :tp nil}}))

(defn ->collector-fn [collector-name f]
  (fn []
    (try
      (log/debug "Collecting metrics" collector-name)
      (f)
      (catch Exception e
        (log/error e "Unable to collect metrics" collector-name)))))

(defn running? [collector-name]
  (some? (get-in @collectors [collector-name :tp])))

(defn conditionally-include-collector [collector-name]
  (when (= collector-name :postgresql)
    (require 'wonko-client.collectors.postgresql)
    (let [f (ns-resolve (find-ns 'wonko-client.collectors.postgresql) 'send-metrics)]
      (swap! collectors assoc-in [:postgresql :f] f))))

(defn start [collector-name & {:as options}]
  (when (running? collector-name)
    (throw (ex-info "Collector is already running" {:collector-name collector-name})))
  (log/info "Starting collector" collector-name)
  (conditionally-include-collector collector-name)
  (let [{:keys [f rate-ms]} (merge (get @collectors collector-name) options)
        tp (util/create-scheduled-tp (->collector-fn collector-name f) rate-ms)]
    (swap! collectors assoc collector-name {:f f :rate-ms rate-ms :tp tp})
    nil))

(defn stop [collector-name]
  (when (not (running? collector-name))
    (throw (ex-info "The collector isn't running" {:collector-name collector-name})))
  (log/info "Stopping collector" collector-name)
  (util/stop-tp (get-in @collectors [collector-name :tp]))
  (swap! collectors update collector-name dissoc :tp)
  nil)

(defn stop-all []
  (doseq [collector-name (filter running? (keys @collectors))]
    (stop collector-name)))
