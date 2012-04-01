(ns server.instrumentations.del
  (:use [server.utils :only [clj->js clj->json]])
  (:require [server.storage :as storage]
            [server.http :as http]))

(defn remove-at [n coll]
  (concat
   (take n coll)
   (drop (inc n) coll)))

(defn handle [resource request response account id]
  (swap! storage/data update-in [:users account :instrumentations]
         #(remove-at (js/parseInt id) %))
  (http/write response 200
              {"Content-Type" "application/json"}
              ""))
