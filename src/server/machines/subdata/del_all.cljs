(ns server.machines.subdata.del_all
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
       (http/error response error)
       (vm/update
        uuid
        {(str "remove_" key) (keys ((first vms) key))}
        (fn [error resp]
          (if error
            (http/error response error)
            (http/ok response (clj->json resp)))))))))