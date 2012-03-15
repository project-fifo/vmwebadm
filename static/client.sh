#!/usr/bin/env bash
if [ ! -f db.js ]; then
    cp data/db.js.example db.js
fi
NODE_PATH="jslib:/usr/vm/node_modules/:$NODE_PATH" node js/client.js $*
