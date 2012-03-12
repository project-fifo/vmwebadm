(ns server.routes
  (:require [server.vm :as vm]
            [server.machines.list :as machines.list]
            [server.machines.get :as machines.get]
            [server.machines.stop :as machines.stop]
            [server.machines.create :as machines.create]
            [server.machines.start :as machines.start]
            [server.machines.del :as machines.del]
            [server.machines.reboot :as machines.reboot]
            [server.machines.resize :as machines.resize]
            [server.packages.list :as packages.list]
            [server.packages.get :as packages.get]
            [server.keys.list :as keys.list]
            [server.keys.add :as keys.add]
            [server.http :as http])
  (:use [server.utils :only [clj->js prn-js clj->json]])
  (:use-macros [clojure.core.match.js :only [match]]))

(defn dispatch [resource request response]
  (print (pr-str resource) "\n")
  (let [ext (:ext resource)
        method (:method resource)
        path (:resource resource)
        query (:query resource)
        out (partial (http/res-ext ext) response)
        default-callback (fn [error resp]
                           (if error
                             (out error)
                             (out resp)))]
    (match [method path query]
           [_ [""] _]
           (http/response-text response "root")

           ["GET" [account "keys"] _]
           (http/with-auth resource request response account 
             #(keys.list/handle resource request response account))

           ["POST" [account "keys"] _]
           (http/with-auth resource request response account
             #(keys.add/handle resource request response account))
           
           
           ["GET" [account "machines"] _]
           (http/with-auth resource request response account
             #(machines.list/handle resource request response account))
           ["POST" [account "machines"] _]
           (http/with-auth resource request response account
             #(machines.create/handle resource request response account))
           ["GET" [account "machines" uuid] _]
           (http/with-auth resource request response account
             #(machines.get/handle resource request response uuid))
           ["DELETE" [account "machines" uuid] _]
           (http/with-auth resource request response account
             #(machines.del/handle resource request response uuid))
           ["POST" [account "machines" uuid] {"action" "stop"}]
           (http/with-auth resource request response account
             #(machines.stop/handle resource request response uuid))
           ["POST" [account "machines" uuid] {"action" "start"}]
           (http/with-auth resource request response account
             #(machines.start/handle resource request response uuid))
           ["POST" [account "machines" uuid] {"action" "reboot"}]
           (http/with-auth resource request response account
             #(machines.reboot/handle resource request response uuid))
           ["POST" [account "machines" uuid] {"action" "resize"}]
           (http/with-auth resource request response account
             #(machines.resize/handle resource request response uuid))
           
           ["GET" [account "packages"] _]
           (http/with-auth resource request response account
             #(packages.list/handle resource request response))
           ["GET" [account "packages" name] _]
           (http/with-auth resource request response account
             #(packages.get/handle resource request response name))
           
           [_ p _]    (http/response-text response (str "Uhh can't find that" (pr-str response))))))