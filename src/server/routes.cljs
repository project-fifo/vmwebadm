(ns server.routes
  (:require [server.vm :as vm]
            [server.machines.list :as machines.list]
            [server.http :as http])
  (:use [server.utils :only [clj->js prn-js clj->json]])
  (:use-macros [clojure.core.match.js :only [match]]))



(defn dispatch [resource request response]
  (let [ext (:ext resource)
        method (:method resource)
        path (:resource resource)

        out (partial (http/res-ext ext) response)
        default-callback (fn [error resp]
                           (if error
                             (out error)
                             (out resp)))]
    (match [method path]
           [_ [""]]
           (http/response-text response "root")
           ["GET" [account "machines"]]
           (machines.list/handle resource request response)
           ["PUT" ["vms"]]
           (http/with-reqest-body request response
             (fn [data]
               (vm/create
                data
                default-callback)))           
           ["GET" ["vms" uuid]]
           (vm/lookup uuid default-callback)
           ["DELETE" ["vms" uuid]]
           (vm/delete uuid default-callback)
           ["POST" ["vms" uuid]]
           (http/with-reqest-body request response
             (fn [data]
               (condp = (data "state")
                 "off" (vm/start uuid (fn []))
                 "started" (vm/stop uuid (fn [])))
               (vm/update
                uuid
                (dissoc data "state")
                default-callback)))
           [_ p]    (http/response-text response (str "Uhh can't find that" (pr-str response))))))