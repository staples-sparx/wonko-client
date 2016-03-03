# Wonko Client

Use this to publish monitoring events to Wonko from your project.

## Installation

Add leiningen dependency:
```clojure
[staples-sparx/wonko-client "0.1.0"]
```

It is hosted in the sparx maven repo, so you'll have to add this to `project.clj`:
```
:repositories {"runa-maven-s3" {:url "s3p://runa-maven/releases/"
                                :username [:gpg :env/archiva_username]
                                :passphrase [:gpg :env/archiva_passphrase]}}
```
## Usage

Initialize the client with the service's name, kafka producer configuration and optionally an exception handler.

```clojure
(require '[wonko-client.core :as wonko])

(wonko/init! "service-name"
             {"bootstrap.servers" "127.0.0.1:9092"
              "compression.type" "gzip"
              "linger.ms" 5}
             :exception-handler (fn [response exception]
                                  (prn response exception))
```

Send monitoring events using `counter`s, `gauge`s, and `stream`s.
```clojure
(wonko/counter :event-occurred nil)
(wonko/counter :job-ended {:status :start})

(wonko/gauge :job-stats {:result :success} 107)
(wonko/gauge :thread-pool-size 42)

(wonko/stream :api-call {:status "200"} 5)
(wonko/stream :api-call {:status "400"} 7)
```

Send alerts to pager-duty, email or slack using `alert`s.
```clojure
(wonko/alert :some-alert-name {:alert :info})
```

Host metrics monitoring is built in, you just have to start it.
```clojure
(require '[wonko-client.host-metrics :as w-host-metrics])
(w-host-metrics/start)
```

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
