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
            [server.http :as http])
  (:use [server.utils :only [clj->js prn-js clj->json]])
  (:use-macros [clojure.core.match.js :only [match]]))

(defn dispatch [resource request response]
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

           ["GET" [account "machines"] _]
           (machines.list/handle resource request response account)
           ["POST" [account "machines"] _]
           (machines.create/handle resource request response account)
           ["GET" [account "machines" uuid] _]
           (machines.get/handle resource request response uuid)
           ["DELETE" [account "machines" uuid] _]
           (machines.del/handle resource request response uuid)
           ["POST" [account "machines" uuid] {"action" "stop"}]
           (machines.stop/handle resource request response uuid)
           ["POST" [account "machines" uuid] {"action" "start"}]
           (machines.start/handle resource request response uuid)
           ["POST" [account "machines" uuid] {"action" "reboot"}]
           (machines.reboot/handle resource request response uuid)
           ["POST" [account "machines" uuid] {"action" "resize"}]
           (machines.resize/handle resource request response uuid)
           
           ["GET" [account "packages"] _]
           (packages.list/handle resource request response)
           ["GET" [account "packages" name] _]
           (packages.get/handle resource request response name)
           
           [_ p _]    (http/response-text response (str "Uhh can't find that" (pr-str response))))))