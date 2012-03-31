(ns server.datasets.get
  (:use
   [server.utils :only [clj->js prn-js clj->json transform-keys]]
   [server.datasets.list :only [res-map dsadm]] )
  (:require
   [cljs.nodejs :as node]
   [server.storage :as storage]
   [server.http :as http]))

(defn handle [resource request response uuid]
  (.show
   dsadm
   uuid
   (fn [error dataset]
     (if dataset
       (let [dataset (js->clj dataset)]
         (http/write response 200
                     {"Content-Type" "application/json"}
                     (clj->json (assoc
                                    (transform-keys res-map dataset) "default" false))))
       (http/write response 404
                   {"Content-Type" "application/json"}
                   (clj->json
                    {"not found" uuid}))))))