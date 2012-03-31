(ns server.datasets.get
  (:use
   [server.utils :only [clj->js prn-js clj->json transform-keys]]
   [server.datasets.list :only [make-map dsadm]] )
  (:require
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
                     (clj->json (make-map dataset))))
       (http/write response 404
                   {"Content-Type" "application/json"}
                   (clj->json
                    {"not found" uuid}))))))