# Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.1.5] - 2016-04-28

### Changed
- The interface to starting host-metrics or other collectors now is: `(collectors/start :ping :host-metrics)`.

### Fixed
- Handle errors when sending host metrics.
- Do not send host metrics with nil metric-value.
- Fix validation of alerts.

### Added
- Optional drop-on-reject configuration to reduce impact on service.
- Alerts are synchronous irrespective of drop-on-reject.
- Validation errors are more readable.
- Ping collector that sends a counter every few seconds to track service uptime.
- Track service uptime using JMX runtime mbean.

## [0.1.4] - 2016-04-01

- JSON serialization is asynchronous, enabling use in real-time services like EP
- Default Metadata (host, IP adress) computation is done only once.
- Using a configurable fixed threadpool to do this.

## [0.1.3] - 2016-03-11

- Validate input schema optionally on setting :validate? option.
- Add logging
- NOTE: label-change exceptions are not thrown if :validate? is not true.

## [0.1.2] - 2016-03-08

- Exceptions if label names are changed for a metric (after the first invocation)

## [0.1.1] - 2016-03-03

- Support for streams
- Production to kafka is asynchronous
- Custom error handling

## [0.1.0] - 2016-02-25

- Initial release (for all practical purposes).
- Support for counters, gauges and alerts.
