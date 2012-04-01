(ns server.routes
  (:require
   [server.vm :as vm]
   [server.http :as http]
   
   [server.machines.list :as machines.list]
   [server.machines.get :as machines.get]
   [server.machines.stop :as machines.stop]
   [server.machines.create :as machines.create]
   [server.machines.start :as machines.start]
   [server.machines.del :as machines.del]
   [server.machines.reboot :as machines.reboot]
   [server.machines.resize :as machines.resize]
   [server.machines.subdata.list :as subdata.list]
   [server.machines.subdata.get :as subdata.get]
   [server.machines.subdata.add :as subdata.add]
   [server.machines.subdata.del :as subdata.del]
   [server.machines.subdata.del_all :as subdata.del_all]
   
   [server.packages.list :as packages.list]
   [server.packages.get :as packages.get]
            
   [server.keys.list :as keys.list]
   [server.keys.get :as keys.get]
   [server.keys.add :as keys.add]
   [server.keys.del :as keys.del]

   [server.instrumentations.get :as inst.get]
   [server.instrumentations.list :as inst.list]
   [server.instrumentations.add :as inst.add]
   [server.instrumentations.del :as inst.del]
   
   [server.datasets.list :as datasets.list]
   [server.datasets.get :as datasets.get])
  
  (:use
   [server.utils :only [clj->js prn-js clj->json]])
  (:use-macros
   [clojure.core.match.js :only [match]]))

(defn dispatch [resource request response]
  (print "resource:" (pr-str resource) "\n")
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

                                        ;keys
           ["GET" [account "keys"] _]
           (do
             (print "keys.list" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(keys.list/handle resource request response account)))
           ["GET" [account "keys" id] _]
           (do
             (print "keys.list" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(keys.get/handle resource request response account id)))
           ["DELETE" [account "keys" id] _]
           (do
             (print "keys.list" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(keys.del/handle resource request response account id)))           
           ["POST" [account "keys"] _]
           (do
             (print "keys.add" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(keys.add/handle resource request response account)))

                                        ;instrumentations
           ["GET" [account "analytics" "instrumentations"] _]
           (do
             (print "inst.list" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(inst.list/handle resource request response account)))
           ["GET" [account "analytics" "instrumentations" id] _]
           (do
             (print "inst.get" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(inst.get/handle resource request response account id)))
           ["POST" [account "analytics" "instrumentations"] _]
           (do
             (print "inst.add" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(inst.add/handle resource request response account)))
           ["DELETE" [account "analytics" "instrumentations" id] _]
           (do
             (print "inst.del" (pr-str path) "\n")
             (http/with-auth resource request response account 
               #(inst.del/handle resource request response account id)))
           
                                        ;machines
           ["GET" [account "machines"] _]
           (do
             (print "machines.list" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.list/handle resource request response account)))
           ["POST" [account "machines"] _]
           (do
             (print "machines.create" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.create/handle resource request response account)))
           ["GET" [account "machines" uuid] _]
           (do
             (print "machines.get" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.get/handle resource request response uuid)))
           ["DELETE" [account "machines" uuid] _]
           (do
             (print "machines.del" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.del/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "stop"}]
           (do
             (print "machines.stop" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.stop/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "start"}]
           (do
             (print "machines.start" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.start/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "reboot"}]
           (do
             (print "machines.reboot" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.reboot/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "resize"}]
           (do
             (print "machines.resize" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(machines.resize/handle resource request response uuid)))
           ["GET" [account "machines" uuid "tags"] _]
           (do
             (print "machines.tags.list" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.list/handle "tags" resource request response account uuid)))
           ["GET" [account "machines" uuid "tags" tag] _]
           (do
             (print "machines.tags.get" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.get/handle "tags" resource request response account uuid tag)))
           ["POST" [account "machines" uuid "tags"] _]
           (do
             (print "machines.tags.add" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.add/handle "tags" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "tags"] _]
           (do
             (print "machines.tags.del_all" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.del_all/handle "tags" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "tags" tag] _]
           (do
             (print "machines.tags.del" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.del/handle "tags" resource request response account uuid tag)))
           ["GET" [account "machines" uuid "metadata"] _]
           (do
             (print "machines.metadata.list" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.list/handle "customer_metadata" resource request response account uuid)))
           ["GET" [account "machines" uuid "metadata" tag] _]
           (do
             (print "machines.metadata.get" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.get/handle "customer_metadata" resource request response account uuid tag)))
           ["POST" [account "machines" uuid "metadata"] _]
           (do
             (print "machines.metadata.add" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.add/handle "customer_metadata" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "metadata"] _]
           (do
             (print "machines.metadata.del_all" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.del_all/handle "customer_metadata" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "metadata" tag] _]
           (do
             (print "machines.metadata.del" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(subdata.del/handle "customer_metadata" resource request response account uuid tag)))
                                        ;datasets
           ["GET" [account "datasets"] _]
           (do
             (print "datasets.list" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(datasets.list/handle resource request response)))
           ["GET" [account "datasets" uuid] _]
           (do
             (print "datasets.list" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(datasets.get/handle resource request response uuid)))
                                        ;packages
           ["GET" [account "packages"] _]
           (do
             (print "packages.list" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(packages.list/handle resource request response)))
           ["GET" [account "packages" name] _]
           (do
             (print "packages.get" (pr-str path) "\n")
             (http/with-auth resource request response account
               #(packages.get/handle resource request response name)))
                                        ;fallback
           [_ p _]    (http/write response 200
                                  {"Content-Type" "application/json"}
                                  (clj->json (str "Uhh can't find that" (pr-str response)))))))