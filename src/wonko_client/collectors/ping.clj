(ns wonko-client.collectors.ping
  (:require [wonko-client.core :as client]
            [clojure.tools.logging :as log]))

(defn send-ping []
  (client/counter :ping {}))
