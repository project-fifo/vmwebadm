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
   "requirements" "requirements"})

(defn make-map [d]
  (let [d (transform-keys res-map d)]
    (assoc d
      "default"
      (= (get @storage/data :default-dataset)
         (d "id")))))

(defn handle [resource request response]
  (.listLocal
   dsadm
   (fn [error datasets]
     (let [datasets (js->clj datasets)]
       (http/write response 200
                   {"Content-Type" "application/json"}
                   (clj->json (map
                               make-map
                               datasets)))))))