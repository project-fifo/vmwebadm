(ns server.keys.del
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require
   [server.storage :as storage]
   [server.http :as http]))

(defn handle [resource request response account id]
  (storage/init)
  (swap! storage/data update-in [:users account :keys] #(dissoc % id))
  (storage/save)
  (http/ret
   response
   {"result" "ok"}))