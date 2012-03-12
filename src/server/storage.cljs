(ns server.storage
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [cljs.nodejs :as node]))


(def data (atom {}))

(def fs (node/require "fs"))

(defn- slurp [file]
  (.readFileSync fs file))


(defn save []
  (.writeFileSync fs "db.js" (clj->json @data)))

(defn init []
  (reset! data (js->clj (.parse js/JSON (slurp "db.js")))))