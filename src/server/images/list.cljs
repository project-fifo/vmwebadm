(ns server.images.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn log]])
  (:require [server.vm :as vm]
            [cljs.nodejs :as node]
            [server.storage :as storage]
            [clojure.string :as c.s]
            [server.http :as http]))

(def fs
  (node/require "fs"))

(defn handle [resource request response account]
  (.readdir
   fs
   "data/images"
   (fn [error images]
     (log 4 (pr-str images))
     (http/ret response
               (map #(c.s/replace % #".iso$", "") (js->clj images))))))