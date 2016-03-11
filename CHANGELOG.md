# Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]

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
