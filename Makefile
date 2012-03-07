CLOJURESCRIPT_HOME=./clojurescript
SERVER_FILES=src/server/
CLIENT_FILES=src/client/
CLJSC_CP=lib/*:
DEPLOY_USER=root
DEPLOY_HOST=192.168.0.27
DEPLOY_PATH=/usbkey/server

bootstrap:
	git submodule init
	git submodule update
	cd clojurescript/; ./scripts/bootstrap
	cp lib/* clojurescript/lib/

all: server

clean: clean-server clean-client

clean-server:
	-rm -r out/server/*

clean-client:
	-rm -r out/client/*

run: all
	node out/server/server.js

server:
	$(CLOJURESCRIPT_HOME)/bin/cljsc $(SERVER_FILES) \
	'{:optimizations :simple :pretty-print true :target :nodejs\
          :output-dir "out/server" :output-to "out/server/server.js"}'

client:
	$(CLOJURESCRIPT_HOME)/bin/cljsc $(CLIENT_FILES) \
	'{:optimizations :simple :pretty-print true :target :nodejs\
          :output-dir "out/client" :output-to "out/client/client.js"}'

deploy: all
	scp -r start.sh out/* $(DEPLOY_USER)@$(DEPLOY_HOST):$(DEPLOY_PATH)
