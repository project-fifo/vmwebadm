(ns server.machines.subdata.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn handle [key resource request response account uuid]
  (vm/lookup
   {"uuid" uuid
    "owner_uuid" account}
   {:full true}
   (fn [error vms]
     (if error
       (http/e500 response error)
       (if-let [vm (first vms)]
         (http/ret response
                   (vm key))
         (http/e404 response "VM Not found."))))))