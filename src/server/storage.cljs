(ns server.storage
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [cljs.nodejs :as node]
            [cljs.reader :as reader]))



(def instrumentations (atom {}))

(def data (atom {}))

(def fs (node/require "fs"))

(defn- slurp [file]
  (str (.readFileSync fs file)))

(defn save []
  (.writeFileSync fs "db.clj" (pr-str @data)))

(defn init []
  (reset! data (js->clj (reader/read-string (slurp "db.clj")))))

(js/setInterval
 init (* 30 1000))
