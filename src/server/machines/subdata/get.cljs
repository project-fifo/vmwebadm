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
       (http/e500 response error)
       (if-let [vm (first vms)]
         (http/ret response
                   (get-in vm [key tag]))
         (http/e404 response "VM Not found."))))))