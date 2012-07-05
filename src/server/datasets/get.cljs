(ns server.datasets.get
  (:use
   [server.utils :only [clj->js prn-js clj->json transform-keys]]
   [server.datasets.list :only [make-map imgadm]] )
  (:require
   [server.http :as http]))

(defn handle [resource request response uuid]
  (.show
   imgadm
   uuid
   (fn [error dataset]
     (if dataset
       (let [dataset (js->clj dataset)]
         (http/ret response
                   (make-map dataset)))
       (http/e404 response
                  "Dataset not found.")))))