(ns server.machines.tags.add
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))


(defn- build-spec [data]
  {"set_tags" data})

(defn handle [resource request response account uuid]
  (http/with-reqest-body request
    (fn [data]
      (let [spec {"set_tags" data}]
        (print "specs: " (pr-str spec) "\n")
        (vm/update
         uuid
         spec
         (fn [error resp]
           (if error
             (http/error response error)
             (http/ok response (clj->json {:tags (:query resource)
                                           :resp resp})))))))))