# Wonko Client

Use this to publish monitoring events to Wonko from your project.

## Installation

Add leiningen dependency:
```clojure
[staples-sparx/wonko-client "0.1.0"]
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
(wonko/init! "service-name"
             {"bootstrap.servers" "127.0.0.1:9092"
              "compression.type" "gzip"
              "linger.ms" 5})
;; monitor
(wonko/counter :this-event-happened nil)
(wonko/counter :some-job {:status :start})
(wonko/gauge :some-job-stats {:type :success} 107)
(wonko/counter :some-job {:status :error})
(wonko/stream :get-user-token {:status :200} 5)

;; send alerts
(wonko/alert :some-alert-name {:alert :info})

;; start monitoring host metrics
(require '[wonko-client.host-metrics :as w-host-metrics])
(w-host-metrics/start)
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
