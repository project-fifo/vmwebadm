CLOJURESCRIPT_HOME=./clojurescript
SERVER_FILES=src/server/
CLIENT_FILES=src/client/
CLJSC_CP=lib/*:
DEPLOY_USER=root
DEPLOY_HOST=192.168.155.139
DEPLOY_PATH=/zones
RELEASE_NAME=vmwebadm

all: server client

bootstrap:
	git submodule init
	git submodule update
	cp db.js.example db.js
	cd clojurescript/ && git checkout r971; 
	cd clojurescript/ && ./script/bootstrap
	cp lib/* clojurescript/lib/

clean: clean-server clean-client clean-release

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

deploy: release all
	scp -r $(RELEASE_NAME) $(DEPLOY_USER)@$(DEPLOY_HOST):$(DEPLOY_PATH)

clean-release:
	[ -d $(RELEASE_NAME) ] && rm -rf $(RELEASE_NAME) || true

release: all clean-release
	mkdir -p $(RELEASE_NAME)/js
	cp out/client/client.js out/server/server.js $(RELEASE_NAME)/js
	cp -r client.sh server.sh db.js.example jslib $(RELEASE_NAME)
