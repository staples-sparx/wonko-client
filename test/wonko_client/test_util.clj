(ns wonko-client.test-util)

(defn rand-str [prefix]
  (->> (.nextInt (java.util.concurrent.ThreadLocalRandom/current) 0 999999)
       (str prefix "-")))
