(ns server.keys.get
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require
   [server.storage :as storage]
   [server.http :as http]))

(defn handle [resource request response account id]
  (if-let [key (get-in @storage/data [:users account :keys id])]
    (http/ret response key)
    (http/e404 response)))