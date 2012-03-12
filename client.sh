#!/usr/bin/bash
if [ ! -f db.js ]; then
    cp db.js.example db.js
fi
NODE_PATH="jslib:/usr/vm/node_modules/:$NODE_PATH" node client/client.js $*
