(ns server.machines.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [cljs.nodejs :as node]
            [server.storage :as storage]
            [server.http :as http]))

(def dsadm
  (node/require "dsadm"))

(def qry-map
  {"name" "alias"
   "type" "brand"
   "dataset" "zfs_filesystem"
   "state" "state"
   "memory" "max_physical_memory"
   "tag" (fn [m tags]
           (reduce
            (fn [m [k v]]
              (assoc m k v))
            m tags))
   })

;   tombstone	Number	Include machines destroyed in the last N minutes
;   credentials	Boolean	Whether to include the generated credentials for machines, if present. Defaults to false.

(def res-map
  {"uuid" "id"
   "alias" "name"
   "zonename" (fn [m name]
                (if (not (m "name"))
                  (assoc m "name" name)))
   "brand" "type"
   "state" "state"
   "nics" (fn [m nics]
            (assoc m "ips"
                   (filter #(not= % "dhcp")
                           (map #(get % "ip") nics))))
   "max_physical_memory" "memory"
   "disks" (fn [m disks]
             (assoc m "disk"
                    (reduce
                     + 
                     (map #(/ (get % "size") 1024) disks))))
   "create_timestamp" "created"
   "dataset_uuid" "dataset"
   })

(defn handle [resource request response account]
  (let [qry (:query resource)
        q (transform-keys qry-map qry)]
    (vm/lookup
     (if (get-in @storage/data [:users account :admin])
       q
       (assoc  q "owner_uuid" (get-in @storage/data [:users account :uuid])))
     {:full true}
     (fn [error vms]
       (if error
         (http/error response error)
         (let [limit (qry "limit")
               offset (get qry "offset" 0)
               vms (drop offset vms)
               vms (if limit (take limit vms) vms)
               cnt (count vms)]
           (.listLocal
            dsadm
            (fn [error datasets]
              (let [datasets (js->clj datasets)
                    ds-map (reduce (fn [m ds]
                                     (assoc m (ds "uuid") (ds "urn")))
                                   {}
                                   datasets)]
                (http/write response 200
                            {"Content-Type" "application/json"
                             "x-resource-count" cnt
                             "x-query-limit" (or limit (inc cnt))}
                            (clj->json (map
                                        (fn [m]
                                          (if-let [d (m "dataset")]
                                            (assoc m "dataset" (ds-map d))
                                            m))
                                        (map (partial transform-keys res-map) vms)))))))))))))