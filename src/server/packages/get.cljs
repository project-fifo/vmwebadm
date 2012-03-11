(ns server.packages.get
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require
   [server.packages.list :as packages.list]
   [server.storage :as storage]
   [server.http :as http]))

(defn handle [resource request response name]
  (if-let [package (get-in @storage/data ["packages" name])]
    (http/write response 200
                {"Content-Type" "application/json"}
                (clj->json
                 (assoc
                     (packages.list/prepare-response package)
                   "name" name)))
    (http/not-found response)))