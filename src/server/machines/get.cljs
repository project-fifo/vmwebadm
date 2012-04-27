(ns server.machines.get
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]
            [server.machines.list :as machines.list]))

(defn handle [resource request response uuid]
  (vm/lookup
   {"uuid" uuid}
   {:full true}
   (fn [error vms]
     (pr vms)
     (if error
       (http/error response error)
       (if-let [vm (first vms)]
         (http/ret response
                   (transform-keys machines.list/res-map vm))
         (http/e404 response "VM Not founds."))))))