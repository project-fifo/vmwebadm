#!/usr/bin/env bash
if [ ! -f db.clj ]; then
    cp data/db.clj.example db.clj
fi
NODE_PATH="jslib:/usr/vm/node_modules/:$NODE_PATH" node js/client.js $*
