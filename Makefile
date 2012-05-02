CLOJURESCRIPT_HOME=./clojurescript
SERVER_FILES=src/server/
CLIENT_FILES=src/client/
CLJSC_CP=lib/*:
DEPLOY_USER=root
DEPLOY_HOST=172.16.0.4
DEPLOY_PATH=/opt
RELEASE_NAME=vmwebadm

all: server client

bootstrap:
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
	-rm -rf $(RELEASE_NAME)

clean-zip:
	-rm vmwebadm.zip

release_pre:
	mkdir -p $(RELEASE_NAME)/js

overlay: clean-release all
	-rm -rf overlay
	mkdir -p overlay/lib/svc/method
	mkdir -p overlay/lib/svc/manifest/system/
	mkdir -p overlay/fifo/js
	mkdir -p overlay/usr/sbin
	mkdir -p overlay/var/db/vmwebadm/images
	cp out/client/client.js out/server/server.js overlay/fifo/js
	cp -r static/* jslib overlay/fifo
	rm -r overlay/fifo/jslib/*/.git*
	mv overlay/fifo/data/vmwebadmd.xml overlay/lib/svc/manifest/system/
	mv overlay/fifo/vmwebadmd overlay/lib/svc/method/
	mv overlay/fifo/vmwebadm overlay/usr/sbin
	sed -i overlay/lib/svc/manifest/system/vmwebadmd.xml -e 's;!DEPLOY_PATH!;/lib/svc/method;g'
	sed -i overlay/usr/sbin/vmwebadm -e 's;!DEPLOY_PATH!;/fifo;g'
	sed -i overlay/lib/svc/method/vmwebadmd -e 's;!DEPLOY_PATH!;/fifo;g'


fix_path:
	sed -i $(RELEASE_NAME)/data/vmwebadmd.xml -e 's;!DEPLOY_PATH!;$(DEPLOY_PATH)/$(RELEASE_NAME);g'
	sed -i $(RELEASE_NAME)/vmwebadm -e 's;!DEPLOY_PATH!;$(DEPLOY_PATH)/$(RELEASE_NAME);g'
	sed -i $(RELEASE_NAME)/vmwebadmd -e 's;!DEPLOY_PATH!;$(DEPLOY_PATH)/$(RELEASE_NAME);g'

release_main:
	cp out/client/client.js out/server/server.js $(RELEASE_NAME)/js
	cp -r static/* jslib $(RELEASE_NAME)
	-find $(RELEASE_NAME) -name "*~" -delete

release: clean-release all release_pre release_main fix_path

zip: release
	tar cvzf vmwebadm-0.4.1.tar.bz2 vmwebadm
