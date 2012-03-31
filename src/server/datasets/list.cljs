(ns server.datasets.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require
   [cljs.nodejs :as node]
   [server.storage :as storage]
   [server.http :as http]))

(def dsadm
  (node/require "dsadm"))

(def res-map
  {"uuid" "id"
   "name" "name"
   "os" "os"
   "urn" "urn"
   "type" "type"
   "requirements" "requirements"
   })

(defn handle [resource request response]
  (.listLocal
   dsadm
   (fn [error datasets]
     (let [datasets (js->clj datasets)]
       (http/write response 200
                   {"Content-Type" "application/json"}
                   (clj->json (map
                               (fn [d]
                                 (assoc
                                     (transform-keys res-map d) "default" false))
                               datasets)))))))