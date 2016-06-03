(ns wonko-client.collectors.ping
  (:require [wonko-client.core :as client]))

(defn send-ping []
  (client/counter :ping {}))
