(ns server.machines.resize
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn- assoc-if [m m1 k]
  (if-let [v (m1 "metadata")]
                 (assoc m "metadata" v)
                 m))

(defn- build-spec [data]
  (if-let [package (data "package")]
    (if (= (first package) "{")
      (js->clj (.parse js/JSON package)))))

(defn handle [resource request response uuid]
  (http/with-reqest-body request
    (fn [data]
      (if-let [spec (build-spec (:query resource))]
        (do 
          (vm/update
           uuid
           spec
           (fn [error]
             (if error
               (http/error response error)
               (http/ok response (clj->json {:response "ok"}))))))
        (http/error response (clj->json  {:error "Package not found"}))))))