(defproject staples-sparx/wonko-client "0.1.6-SNAPSHOT-1"
  :local-repo ".m2"
  :description "Clojure client for the Wonko service"
  :url "git@github.com:staples-sparx/wonko-client.git"
  :repositories {"runa-maven-s3" {:url "s3p://runa-maven/releases/"
                                  :username [:gpg :env/archiva_username]
                                  :passphrase [:gpg :env/archiva_passphrase]}}
  :resource-paths ["resources"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.5.0"]
                 [clj-kafka "0.3.4"]
                 [prismatic/schema "1.0.5"]
                 [org.clojure/java.jmx "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.lmax/disruptor "3.3.4"]]
  :plugins [[s3-wagon-private "1.2.0"]
            [cider/cider-nrepl "0.11.0"]
            [refactor-nrepl "2.0.0"]]
  :target-path "target/%s"
  :jvm-opts ["-Xmx1g" "-Xms1g" "-server"]
  :profiles {:uberjar {:aot :all}
             :dev {:resource-paths ["dev-resources"]
                   :dependencies [[throttler "1.0.0"]
                                  [io.dropwizard.metrics/metrics-core "3.1.0"]
                                  [org.apache.logging.log4j/log4j-core "2.5"]
                                  [org.apache.logging.log4j/log4j-slf4j-impl "2.5"]
                                  [org.slf4j/slf4j-api "1.7.15"]]}})
