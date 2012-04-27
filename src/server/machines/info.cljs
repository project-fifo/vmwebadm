(ns server.machines.info
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]
            [server.machines.list :as machines.list]))


(def res-map
  {"vnc" "vnc"
   "cpus" (fn [m cpus]
             (assoc m "vcpus" (count cpus)))})

(defn handle [resource request response uuid]
  (vm/lookup
   {"uuid" uuid}
   {:full true}
   (fn [error vms]
     (if error
       (http/error response error)
       (if-let [vm (first vms)]
         (cond
          (not= (vm "brand") "kvm")
          (http/e405 response "Not valid for zone.")
          (not= (vm  "state") "running")
          (http/e405 response "Not supported on powered of machines.")
          :else
          (vm/info uuid
                   (fn [error info]
                     (if error
                       (http/error response error)
                       (http/ret response
                                 (transform-keys res-map info))))))
         (http/e404 response "VM Not founds."))))))