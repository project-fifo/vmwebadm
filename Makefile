CLOJURESCRIPT_HOME=./clojurescript
SERVER_FILES=src/server/
CLIENT_FILES=src/client/
CLJSC_CP=lib/*:
DEPLOY_USER=root
DEPLOY_HOST=192.168.155.139
DEPLOY_PATH=/zones

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

deploy: all
	scp -r release $(DEPLOY_USER)@$(DEPLOY_HOST):$(DEPLOY_PATH)/vmwebadmin

clean-release:
	rm -rf release

release: all clean-release
	mkdir -p release/js
	cp out/client/client.js out/server/server.js release/js
	cp -r client.sh server.sh db.js.example jslib release
