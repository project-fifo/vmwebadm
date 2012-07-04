SERVER_SRC_FILES=$(shell find src/server -name "*.cljs")
CLIENT_SRC_FILES=$(shell find src/client -name "*.cljs")
CLOJURESCRIPT_HOME=./clojurescript
SERVER_FILES=src/server/
CLIENT_FILES=src/client/
CLJSC_CP=lib/*:
DEPLOY_USER=root
DEPLOY_HOST=172.16.0.4
DEPLOY_PATH=/opt
RELEASE_NAME=vmwebadm
RELEASE_VERSION=0.4.4

all: out/server/server.js out/client/client.js

$(CLOJURESCRIPT_HOME)/bin/cljsc:
	git submodule init
	git submodule update
	cd clojurescript/ && ./script/bootstrap
	cp lib/* clojurescript/lib/


clean: clean-server clean-client clean-release clean-zip

clean-server:
	-rm -r out/server/*

clean-client:
	-rm -r out/client/*

run: all
	node out/server/server.js

out/server/server.js: $(SERVER_SRC_FILES) $(CLOJURESCRIPT_HOME)/bin/cljsc 
	$(CLOJURESCRIPT_HOME)/bin/cljsc $(SERVER_FILES) \
	'{:optimizations :simple :pretty-print true :target :nodejs :output-dir "out/server" :output-to "out/server/server.js"}'

out/client/client.js: $(CLIENT_SRC_FILES) $(CLOJURESCRIPT_HOME)/bin/cljsc
	$(CLOJURESCRIPT_HOME)/bin/cljsc $(CLIENT_FILES) \
	'{:optimizations :simple :pretty-print true :target :nodejs :output-dir "out/client" :output-to "out/client/client.js"}'

deploy: rel all
	scp -r $(RELEASE_NAME) $(DEPLOY_USER)@$(DEPLOY_HOST):$(DEPLOY_PATH)

clean-release:
	-rm -rf $(RELEASE_NAME)

clean-zip:
	-rm vmwebadm.zip

release_pre:
	mkdir -p $(RELEASE_NAME)/js

fix_path:
	sed -i $(RELEASE_NAME)/data/vmwebadmd.xml -e 's;!DEPLOY_PATH!;$(DEPLOY_PATH)/$(RELEASE_NAME);g'
	sed -i $(RELEASE_NAME)/vmwebadm -e 's;!DEPLOY_PATH!;$(DEPLOY_PATH)/$(RELEASE_NAME);g'
	sed -i $(RELEASE_NAME)/vmwebadmd -e 's;!DEPLOY_PATH!;$(DEPLOY_PATH)/$(RELEASE_NAME);g'

release_main:
	cp out/client/client.js out/server/server.js $(RELEASE_NAME)/js
	cp -r static/* jslib $(RELEASE_NAME)
	-find $(RELEASE_NAME) -name "*~" -delete

rel: clean-release all release_pre release_main fix_path

tar: rel
	tar cvzf vmwebadm-$(RELEASE_VERSION).tar.bz2 vmwebadm
