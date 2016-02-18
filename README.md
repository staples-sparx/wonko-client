# Wonko Client

Use this to publish monitoring events to Wonko from your project.

## Installation

Add leiningen dependency:
```clojure
[staples-sparx/wonko-client "0.1.1-SNAPSHOT"]
```

So you can pull from the sparx maven repo:
```
:repositories {"runa-maven-s3" {:url "s3p://runa-maven/releases/"
                                :username [:gpg :env/archiva_username]
                                :passphrase [:gpg :env/archiva_passphrase]}}
```
## Usage

```clojure
(require '[wonko-client.core :as wonko])

;; init
(wonko/init! {:topic "kafka-topic"
              :producer {"bootstrap.servers" "127.0.0.1:9092"
                         "compression.type" "gzip"
                         "linger.ms" 5}})
;; monitor
(wonko/counter :this-event-happened nil)
(wonko/counter :some-job {:status :start})
(wonko/gauge :some-job-stats {:type :success} 107)
(wonko/counter :some-job {:status :error} :alert true)
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
