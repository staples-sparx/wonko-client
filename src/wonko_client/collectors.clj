(ns wonko-client.collectors
  (:require [clojure.tools.logging :as log]
            [wonko-client.collectors.host-metrics :as hm]
            [wonko-client.collectors.ping :as ping]
            [wonko-client.util :as util]))

(def default-config
  {:host-metrics {:rate-ms 5000}
   :ping         {:rate-ms 5000}
   :postgresql   {:rate-ms 60000}})

(defonce collectors
  ^{:doc "Map name of collector to the thread(pool) that it runs in."}
  (atom
   {:host-metrics nil
    :ping nil
    :postgresql nil}))

(defn ->collector-fn [collector-name f]
  (fn []
    (try
      (log/debug "Collecting metrics" collector-name)
      (f)
      (catch Exception e
        (log/error e "Unable to collect metrics" collector-name)))))

(defn running? [collector-name]
  (some? (get @collectors collector-name)))

(defn start [collector-name collect-fn options]
  (if (running? collector-name)
    (log/warn "The collector is already running" {:collector-name collector-name})
    (let [{:keys [rate-ms] :as config} (merge (get default-config collector-name) options)
          tp (util/create-scheduled-tp (->collector-fn collector-name collect-fn) rate-ms)]
      (log/info "Started collector" collector-name "with config" config)
      (swap! collectors assoc collector-name tp)
      nil)))

(defn stop [collector-name]
  (if (not (running? collector-name))
    (log/warn "The collector isn't running" {:collector-name collector-name})
    (do
      (util/stop-tp (get @collectors collector-name))
      (log/info "Stopped collector" collector-name)
      (swap! collectors dissoc collector-name)
      nil)))

(defn start-host-metrics [& {:as options}]
  (start :host-metrics hm/send-metrics options))

(defn start-ping [& {:as options}]
  (start :ping ping/send-ping options))

(defn start-postgresql [get-conn-fn & {:as options}]
  (require 'wonko-client.collectors.postgresql)
  (let [collect-fn (ns-resolve (find-ns 'wonko-client.collectors.postgresql) 'send-metrics)]
    (start :postgresql #(collect-fn get-conn-fn) options)))

(defn stop-all []
  (doseq [collector-name (keys @collectors)]
    (stop collector-name)))
