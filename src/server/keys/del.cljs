(ns server.keys.del
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require
   [server.storage :as storage]
   [server.http :as http]))

(defn handle [resource request response account id]
  (swap! storage/data update-in [:users account :keys] #(dissoc % id))
  (http/write
   response 200
   {"Content-Type" "application/json"}
   (clj->json
    {"result" "ok"})))