CLOJURESCRIPT_HOME=./clojurescript
SERVER_FILES=src/server/
CLIENT_FILES=src/client/
CLJSC_CP=lib/*:
DEPLOY_USER=root
DEPLOY_HOST=192.168.155.139
DEPLOY_PATH=/zones/server

all: server

bootstrap:
	git submodule init
	git submodule update
	cd clojurescript/ && git checkout r971; 
	cd clojurescript/ && ./script/bootstrap
	cp lib/* clojurescript/lib/

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
