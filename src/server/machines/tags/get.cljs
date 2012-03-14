(ns server.machines.tags.get
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn handle [resource request response account uuid tag]
  (print uuid "-" account "-" tag "\n")
  (vm/lookup
   {"uuid" uuid
    "owner_uuid" account}
   {:full true}
   (fn [error vms]
     (if error
       (http/error response error)
       (http/write response 200
                   {"Content-Type" "application/json"}
                   (clj->json (get-in (first vms) ["tags" tag])))))))