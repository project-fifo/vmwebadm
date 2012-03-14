(ns server.machines.tags.del_all
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))


(defn- build-spec [data]
  {"set_tags" data})

(defn handle [resource request response account uuid]
  (vm/lookup
   {"uuid" uuid
    "owner_uuid" account}
   {:full true}
   (fn [error vms]
     (if error
       (http/error response error)
       (vm/update
        uuid
        {"remove_tags" (keys ((first vms) "tags"))}
        (fn [error resp]
          (if error
            (http/error response error)
            (http/ok response (clj->json resp)))))))))