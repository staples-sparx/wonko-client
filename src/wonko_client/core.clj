(ns wonko-client.core
  (:require [clojure.tools.logging :as log]
            [wonko-client.abq :as abq]
            [wonko-client.disruptor :as disruptor]
            [wonko-client.kafka-producer :as kp]
            [wonko-client.message :as message]
            [wonko-client.message.validation :as v]))

(defn default-exception-handler [e message response]
  (log/error e {:message message :response response :e (bean e)}))

(def ^:private default-options
  {:validate?         false
   :drop-on-reject?   false
   :exception-handler default-exception-handler
   :worker-count      10
   :queue-size        10
   :topics            {:events "wonko-events"
                       :alerts "wonko-alerts"}})

(defonce instance
  {:service nil
   :topics nil
   :queue nil
   :producer nil})

(comment
  (def q-send-async abq/send-async)
  (def q-init abq/init)
  (def q-terminate abq/terminate))

(def q-send-async disruptor/send-async)
(def q-init disruptor/init)
(def q-terminate disruptor/terminate)

(defn counter [metric-name properties & {:as options}]
  (->> :counter
       (message/build (:service instance) metric-name properties nil options)
       (q-send-async instance :events)))

(defn gauge [metric-name properties metric-value & {:as options}]
  (->> :gauge
       (message/build (:service instance) metric-name properties metric-value options)
       (q-send-async instance :events)))

(defn stream [metric-name properties metric-value & {:as options}]
  (->> :stream
       (message/build (:service instance) metric-name properties metric-value options)
       (q-send-async instance :events)))

(defn alert [alert-name alert-info]
  (->> :counter
       (message/build-alert (:service instance) alert-name {} nil alert-name alert-info nil)
       (kp/send instance :alerts)))

(defn init! [service-name kafka-config & {:as user-options}]
  (let [options (merge default-options user-options)
        {:keys [validate? topics]} options]
    (alter-var-root #'instance
                    (constantly
                     {:service service-name
                      :topics topics
                      :queue (q-init options)
                      :producer (kp/create kafka-config)
                      :exception-handler (:exception-handler options)}))
    (v/set-validation! validate?)
    (log/info "wonko-client initialized" instance)
    nil))

(defn terminate! []
  (q-terminate instance)
  (kp/close instance)
  nil)
