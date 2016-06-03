# Wonko Client

Use this to publish monitoring events to Wonko from your project.

## Installation

Add leiningen dependency:
```clojure
[staples-sparx/wonko-client "0.1.6"]
```

It is hosted in SparX s3 maven, so you'll have to add this to `project.clj`:
```
:repositories {"runa-maven-s3" {:url "s3p://runa-maven/releases/"
                                :username [:gpg :env/archiva_username]
                                :passphrase [:gpg :env/archiva_passphrase]}}
```
## Usage

Initialize the client with the service's name, kafka producer configuration and optionally an exception handler. Validation is turned off by default (for performance reasons), but you can enable validation in dev and test environments to ensure inputs are correct.

```clojure
(require '[wonko-client.core :as wonko])

(wonko/init! "service-name"
             {"bootstrap.servers" "127.0.0.1:9092"
              "compression.type" "gzip"
              "linger.ms" 5
              "block.on.buffer.full" "false"
              "total.memory.bytes" (* 1024 1024 120)}
             :exception-handler (fn [exception message response]
                                  (prn response message exception))
             :validate? true)
```

Send monitoring events using `counter`s, `gauge`s, and `stream`s.
```clojure
(wonko/counter :event-occurred nil)
(wonko/counter :job-ended {:status :start})

(wonko/gauge :job-stats {:result :success} 107)
(wonko/gauge :worker-count 42)

(wonko/stream :api-call {:status "200"} 5)
(wonko/stream :api-call {:status "400"} 7)
```

Send alerts to pager-duty, email or slack using `alert`s.
```clojure
(wonko/alert :some-alert-name {:alert :info})
```

### Collectors

These are scheduled threads that collect come metrics at a specified `rate-ms`. You can start them from the collectors namespace:
```clojure
(require '[wonko-client.collectors :as wc])
```

There are a few in built collectors:

1. `ping`
  - This is a simple a counter, which acts as a heartbeat for the service. Presence/absence of this can be used to detect status of the application.
  - To start, use `(wc/start-ping)`
  - This runs every 5 seconds by default.
2. `host-metrics`
  - This monitors memory, cpu, disk, gc, uptime, etc. There's almost no reason to not start this collector in your application.
  - To start, use `(wc/start-host-metrics)`
  - This runs every 5 seconds by default.
3. `postgresql`
  - Sends metrics about queries, cache hits, disk usage, bloat, indexes, vacuum, etc. This requires `clojure.java.jdbc` in the application's classpath.
  - To start, use `(wc/start-postgresql get-conn-fn)` where `get-conn-fn` is a function that wonko-client can use to get a jdbc-connection to the postgresql database.
  - This runs every 1 minute by default.

To change the default rates at which these run, use the optional keyword argument `:rate-ms`.
```clojure
(wc/start-postgresql get-conn-fn :rate-ms (* 5 60 1000)) ;; run every 5 minutes
```

## Options

- `validate?`: Set this to true in dev environments to synchronously validate schemas of arguments to wonko metrics. Wonko-client will throw schema exception IllegalArgumentException with a description of the errors. Default value is `false`.
- `worker-count` and `queue-size`: These are the configs for a fixed threadpool within wonko that makes a few things asynchronous. Typically, you wouldn't need to tune these. Default values are `10` and `10` respectively.
- `:drop-on-reject?`: Set this to true if slowing down the service in case of a problem with wonko-client/kafka is unacceptable. This options allows you to choose between dropping metrics and adding back pressure to the application. Alerts are synchronous however, so they will not be dropped. The thread-pool itself can be tuned such that under normal conditions, no metrics will be dropped.
- `:exception-handler`: This is a 3 argument function that is called when there is an exception within the queueing mechanism or in sending a message to kafka. The arguments are: `exception`, `message`, and `response`, where `response` is the response from kafka describing the failure if available.

## Metric types
### Counter
A counter is a simple incrementing number. It only ever goes up. You can compute the rate (number of events per second) at which a counter is changing, and that is usually more useful than the value itself.

They are useful for counting things like requests, task started/ended, errors/alerts, etc. Consider adding counters along with log statements, and failure occurrences. If the value can go down, pick a gauge.
### Gauge
A gauge is a numerical value that can go up or down. Consider using a gauge to monitor in-progress requests, queue size, current thread count, pending jobs, batch-job timing, etc. Averages and rates of gauges are usually meaningless.

Not every value of a gauge is reported on (because prometheus polls the data from wonko, gauge changes between polls are lost). If you want to track a series of values, use a Stream instead.
### Stream
A stream is series of values, which are observations of a metric. All values in a stream are used for sampling and aggregating. You can use streams to compute rate, distributions/quantiles and aggregates.

Typically, request latencies, feed lengths, SLA computations, and any kind of performance measurement would warrant a stream.

## Properties
These are characteristics of events being monitored. URIs, response statuses, different stages in pieline, etc can be tracked using properties. You can filter metrics using properties, and aggregate across them.

Ensure that property values are a bounded set. Properties are intended to be metadata for a given metric - if you want to send unbounded _values_ to Wonko, make them the primary metric value.
For example, "Response Status Code" can be a property, since there are a finite number of values it can take. "Response time" should not be a property, but a primary metric value.

## Alerts
An alert is used to notify people via pager-duty, email or slack. Consider using this for any failure scenario for which you want to be notified. An alert is also implicitly a counter, so you can use it to get stats on alerts over time.

For (more comprehensive) alerts based on statistical or historical data, consider configuring them through prometheus.

## License
Copyright © 2016 Staples Sparx.
Released under the MIT license.
http://mit-license.org/
