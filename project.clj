(defproject staples-sparx/wonko-client "0.1.2"
  :local-repo ".m2"
  :description "Clojure client for the Wonko service"
  :url "git@github.com:staples-sparx/wonko-client.git"
  :repositories {"runa-maven-s3" {:url "s3p://runa-maven/releases/"
                                  :username [:gpg :env/archiva_username]
                                  :passphrase [:gpg :env/archiva_passphrase]}}
  :dependencies [[cheshire "5.5.0"]
                 [clj-kafka "0.3.4"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jmx "0.3.1"]]
  :plugins [[s3-wagon-private "1.2.0"]
            [cider/cider-nrepl "0.11.0"]
            [refactor-nrepl "2.0.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
