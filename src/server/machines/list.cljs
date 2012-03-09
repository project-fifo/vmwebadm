(ns server.machines.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))


(def qry-map
  {"name" "zonename"
   "type" "brand"
   "dataset" "zfs_filesystem"
   "state" "state"
   "memory" "max_physical_memory"})

;   tag.$name	String	An arbitrary set of tags can be used to query on, assuming they are prefixed with "tag."
;   tombstone	Number	Include machines destroyed in the last N minutes
;   credentials	Boolean	Whether to include the generated credentials for machines, if present. Defaults to false.

(def res-map
  {"uuid" "id"
   "zonename" "name"
   "brand" "type"
   "state" "state"
   "nics" ["ips" (fn [nics]
                   (filter #(not= "dhcp")
                           (map #(get % "ip") nics)))]
   "max_physical_memory" "memory"
   "disks" ["disk"
            (fn [disks]
              (reduce + 
                      (map #(/ (get % "size") 1024) disks)))]
   "create_timestamp" "create"
   "zfs_filesystem" "dataset" 
   })

(defn handle [resource request response]
  (let [qry (:query resource)
        q (transform-keys qry-map qry)]
    (vm/lookup
     q
     {:full true}
     (fn [error vms]
       (if error
         (default-callback error vms)
         (let [limit (qry "limit")
               offset (get qry "offset" 0)
               vms (drop offset vms)
               vms (if limit (take limit vms) vms)
               cnt (count vms)]
           (http/write response 200
                       {"Content-Type" "application/json"
                        "x-resource-count" cnt
                        "x-query-limit" (or limit (inc cnt))}
                       (clj->json (map (partial transform-keys res-map) vms)))))))))