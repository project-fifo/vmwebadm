(ns server.instrumentations.del
  (:use [server.utils :only [clj->js clj->json]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.http :as http]))

(defn remove-at [n coll]
  (concat
   (take n coll)
   (drop (inc n) coll)))

(defn handle [resource request response account id]
  (let [consumer (get-in @storage/instrumentations [account id :consumer])]
    (if consumer (dtrace/stop consumer))
    (swap! storage/instrumentations update-in [account]
           #(remove-at id %))
    (http/write response 200
                {"Content-Type" "application/json"}
                "")))