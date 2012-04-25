(ns server.machines.del
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.storage :as storage]
            [server.http :as http]))

(defn handle [resource request response uuid]
  (vm/lookup
   {"uuid" uuid}
   {:full true}
   (fn [error vms]
     (if error
       (http/e500 response error)
       (if-let [vm (first vms)]
         (vm/del
          uuid
          (fn [error]
            (if error
              (http/e500 response error)
              (do
                (doseq [nic (vm "nics")]
                  (if (= "admin" (nic "nic_tag"))
                    (storage/reclaim-ip :admin (nic "ip"))
                    (storage/reclaim-ip :ext (nic "ip"))))
                (http/ret response "deleted.")))))
         (http/e404 response "VM Not found"))))))