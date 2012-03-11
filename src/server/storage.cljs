(ns server.storage
  (:require [cljs.nodejs :as node]))

(def data (atom {}))

(def fs (node/require "fs"))

(defn slurp [file]
  (.readFileSync fs file))

(defn init []
  (reset! data (js->clj (.parse js/JSON (slurp "db.js")))))