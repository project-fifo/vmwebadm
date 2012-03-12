(ns server.packages.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [server.http :as http]))


(def res-map
  {"quota" "disk"
   "max_physical_memory" "memory"
   "max_swap" "swap"})

(defn- prepare-response [m]
  (transform-keys
   res-map 
   (if (m "max_swap")
     m
     (assoc m "max_swap" (m "max_physical_memory")))))



(defn handle [resource request response]
  (http/write response 200
              {"Content-Type" "application/json"}
              (clj->json
               (reduce
                (fn [l [n m]]
                  (conj l
                        (assoc (prepare-response m) "name" n)))
                []
                (@storage/data "packages")))))