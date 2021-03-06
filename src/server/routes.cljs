(ns server.routes
  (:require
   [server.vm :as vm]
   [server.http :as http]
   [server.storage :as storage]
   
   [server.machines.list :as machines.list]
   [server.machines.get :as machines.get]
   [server.machines.info :as machines.info]
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

   [server.images.list :as images.list]
   
   [server.keys.list :as keys.list]
   [server.keys.get :as keys.get]
   [server.keys.add :as keys.add]
   [server.keys.del :as keys.del]

   [server.instrumentations.desc :as inst.desc]
   [server.instrumentations.getval :as inst.getval]
   [server.instrumentations.getheatmap :as inst.getheatmap]
   [server.instrumentations.get :as inst.get]
   [server.instrumentations.list :as inst.list]
   [server.instrumentations.add :as inst.add]
   [server.instrumentations.del :as inst.del]
   
   [server.datasets.list :as datasets.list]
   [server.datasets.get :as datasets.get])
  
  (:use
   [server.utils :only [clj->js prn-js clj->json log]])

  (:use-macros
   [clojure.core.match.js :only [match]]))

(defn dispatch [resource request response]
  (log 4 "resource:" (pr-str resource))
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
             (log 2 "keys.list" (pr-str path))
             (http/with-auth resource request response account 
               #(keys.list/handle resource request response account)))
           ["GET" [account "keys" id] _]
           (do
             (log 2 "keys.list" (pr-str path))
             (http/with-auth resource request response account 
               #(keys.get/handle resource request response account id)))
           ["DELETE" [account "keys" id] _]
           (do
             (log 2 "keys.list" (pr-str path))
             (http/with-auth resource request response account 
               #(keys.del/handle resource request response account id)))           
           ["POST" [account "keys"] _]
           (do
             (log 2 "keys.add" (pr-str path))
             (http/with-auth resource request response account
               #(keys.add/handle resource request response account)))

                                        ;instrumentations
           ["GET" [account "analytics"] _]
           (do
             (log 2 "inst.desc" (pr-str path))
             (http/with-auth resource request response account 
               #(inst.desc/handle resource request response account)))
           ["GET" [account "analytics" "instrumentations"] _]
           (do
             (log 2 "inst.list" (pr-str path))
             (http/with-auth resource request response account 
               #(inst.list/handle resource request response account)))
           ["GET" [account "analytics" "instrumentations" id] _]
           (do
             (log 2 "inst.get" (pr-str path))
             (http/with-auth resource request response account 
               #(inst.get/handle resource request response account (dec (js/parseInt id)))))
           ["POST" [account "analytics" "instrumentations"] _]
           (do
             (log 2 "inst.add" (pr-str path))
             (http/with-auth resource request response account 
               #(inst.add/handle resource request response account)))
           ["DELETE" [account "analytics" "instrumentations" id] _]
           (do
             (log 2 "inst.del" (pr-str path))
             (http/with-auth resource request response account
               #(inst.del/handle resource request response account (dec (js/parseInt id)))))
           
           ["GET" [account "analytics" "instrumentations" id "value" "raw"] _]
           (do
             (log 2 "inst.get-val" (pr-str path))
             (http/with-auth resource request response account 
               #(inst.getval/handle resource request response account (dec (js/parseInt id)))))

           ["GET" [account "analytics" "instrumentations" id "value" "heatmap" "image"] _]
           (do
             (log 2 "inst.get-val" (pr-str path))
             (http/with-auth resource request response account 
               #(inst.getheatmap/handle resource request response account (dec (js/parseInt id)))))


                                        ;machines
           ["GET" [account "machines"] _]
           (do
             (log 2 "machines.list" (pr-str path))
             (http/with-auth resource request response account
               #(machines.list/handle resource request response account)))
           ["POST" [account "machines"] _]
           (do
             (log 2 "machines.create" (pr-str path))
             (http/with-auth resource request response account
               #(machines.create/handle resource request response account)))
           ["GET" [account "machines" uuid] _]
           (do
             (log 2 "machines.get" (pr-str path))
             (http/with-auth resource request response account
               #(machines.get/handle resource request response uuid)))
           ["GET" [account "machines" uuid "info"] _]
           (do
             (log 2 "machines.info" (pr-str path))
             (http/with-auth resource request response account
               #(machines.info/handle resource request response uuid)))
           ["DELETE" [account "machines" uuid] _]
           (do
             (log 2 "machines.del" (pr-str path))
             (http/with-auth resource request response account
               #(machines.del/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "stop"}]
           (do
             (log 2 "machines.stop" (pr-str path))
             (http/with-auth resource request response account
               #(machines.stop/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "start"}]
           (do
             (log 2 "machines.start" (pr-str path))
             (http/with-auth resource request response account
               #(machines.start/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "reboot"}]
           (do
             (log 2 "machines.reboot" (pr-str path))
             (http/with-auth resource request response account
               #(machines.reboot/handle resource request response uuid)))
           ["POST" [account "machines" uuid] {"action" "resize"}]
           (do
             (log 2 "machines.resize" (pr-str path))
             (http/with-auth resource request response account
               #(machines.resize/handle resource request response uuid)))
           ["GET" [account "machines" uuid "tags"] _]
           (do
             (log 2 "machines.tags.list" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.list/handle "tags" resource request response account uuid)))
           ["GET" [account "machines" uuid "tags" tag] _]
           (do
             (log 2 "machines.tags.get" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.get/handle "tags" resource request response account uuid tag)))
           ["POST" [account "machines" uuid "tags"] _]
           (do
             (log 2 "machines.tags.add" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.add/handle "tags" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "tags"] _]
           (do
             (log 2 "machines.tags.del_all" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.del_all/handle "tags" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "tags" tag] _]
           (do
             (log 2 "machines.tags.del" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.del/handle "tags" resource request response account uuid tag)))
           ["GET" [account "machines" uuid "metadata"] _]
           (do
             (log 2 "machines.metadata.list" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.list/handle "customer_metadata" resource request response account uuid)))
           ["GET" [account "machines" uuid "metadata" tag] _]
           (do
             (log 2 "machines.metadata.get" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.get/handle "customer_metadata" resource request response account uuid tag)))
           ["POST" [account "machines" uuid "metadata"] _]
           (do
             (log 2 "machines.metadata.add" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.add/handle "customer_metadata" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "metadata"] _]
           (do
             (log 2 "machines.metadata.del_all" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.del_all/handle "customer_metadata" resource request response account uuid)))
           ["DELETE" [account "machines" uuid "metadata" tag] _]
           (do
             (log 2 "machines.metadata.del" (pr-str path))
             (http/with-auth resource request response account
               #(subdata.del/handle "customer_metadata" resource request response account uuid tag)))
                                        ;datasets
           ["GET" [account "datasets"] _]
           (do
             (log 2 "datasets.list" (pr-str path))
             (http/with-auth resource request response account
               #(datasets.list/handle resource request response)))
           ["GET" [account "datasets" uuid] _]
           (do
             (log 2 "datasets.list" (pr-str path))
             (http/with-auth resource request response account
               #(datasets.get/handle resource request response uuid)))
                                        ;packages
           ["GET" [account "packages"] _]
           (do
             (log 2 "packages.list" (pr-str path))
             (http/with-auth resource request response account
               #(packages.list/handle resource request response)))
           ["GET" [account "packages" name] _]
           (do
             (log 2 "packages.get" (pr-str path))
             (http/with-auth resource request response account
               #(packages.get/handle resource request response name)))

           ["GET" [account "images"] _]
           (do
             (log 2 "images.list" (pr-str path))
             (http/with-auth resource request response account
               #(images.list/handle resource request response)))

                                        ;fallback
           [_ p _]
           (http/e404
            (str "Uhh can't find that: " (pr-str p))))))