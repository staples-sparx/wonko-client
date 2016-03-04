.PHONY:	env tests deploy ci

ARCHIVA_USERNAME = $(shell grep access_key ~/.s3cfg | head -n1 | awk -F ' = ' '{print $$2 }')
ARCHIVA_PASSPHRASE = $(shell grep secret_key ~/.s3cfg | head -n1 | awk -F ' = ' '{print $$2}')

LEIN = HTTP_CLIENT="curl --insecure -f -L -o" lein

LEIN_ENV=ARCHIVA_USERNAME="${ARCHIVA_USERNAME}" ARCHIVA_PASSPHRASE="${ARCHIVA_PASSPHRASE}"

all: deps lein-deps

distclean:
	rm -rf ./.m2

clean:
	$(LEIN_ENV) $(LEIN) clean

lein-deps:
	$(LEIN_ENV) $(LEIN) deps

env:
	@echo "ARCHIVA_USERNAME=$(ARCHIVA_USERNAME)"
	@echo "ARCHIVA_PASSPHRASE=$(ARCHIVA_PASSPHRASE)"

start-ci-services: deps
	./bin/deps start zookeeper
	./bin/deps start kafka

stop-ci-services:
	-./bin/deps stop kafka
	-./bin/deps stop zookeeper

ci: lein-deps
	make start-ci-services
	make tests

# FIXME
# (Probably) because of an ungraceful exit, kafka would
# encounter a conflict (INFO conflict in /brokers/ids/0 data)
# and refuse to boot on alternate runs. A quick fix is to zap
# zookeeper and kafka's data directories after every run, but
# this should be fixed to get at the real issue (probably making
# the exit more graceful).
ci-clean:
	-make stop-ci-services
	-rm -rf /tmp/kafka-logs
	-rm -rf /tmp/zookeeper

tests: lein-deps
	$(LEIN_ENV) $(LEIN) test

deploy:
	$(LEIN_ENV) $(LEIN) deploy runa-maven-s3

deps:
	./bin/deps install all

deps-check:
	./bin/deps check all

deps-pull:
	git submodule update --init
