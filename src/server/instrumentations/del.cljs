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
  (if (get-in @storage/instrumentations [account id])
    (let [consumer (get-in @storage/instrumentations [account id :consumer])]
      (if consumer (dtrace/stop consumer))
      (swap! storage/instrumentations update-in [account]
             #(remove-at id %))
      (http/ret response "Deleted."))
    (http/e404 response "Instrumentation not found.")))