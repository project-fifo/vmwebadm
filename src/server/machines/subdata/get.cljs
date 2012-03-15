(ns server.machines.subdata.get
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn handle [key resource request response account uuid tag]
  (vm/lookup
   {"uuid" uuid
    "owner_uuid" account}
   {:full true}
   (fn [error vms]
     (if error
       (http/error response error)
       (http/write response 200
                   {"Content-Type" "application/json"}
                   (clj->json (get-in (first vms) [key tag])))))))