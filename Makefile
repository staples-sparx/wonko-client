.PHONY:	env tests deploy

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

ci: distclean
	make tests

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
