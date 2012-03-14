(ns server.machines.tags.del
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))


(defn- build-spec [data]
  {"set_tags" data})

(defn handle [resource request response account uuid tag]
  (if-let [spec {"remove_tags" [tag]}]
    (do 
      (vm/update
       uuid
       spec
       (fn [error resp]
         (if error
           (http/error response error)
           (http/ok response (clj->json {:tags (:query resource)
                                         :resp resp}))))))
    (http/error response (clj->json  {:error "Package not found"}))))