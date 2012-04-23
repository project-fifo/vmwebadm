(ns server.instrumentations.get
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [server.http :as http]))

(defn handle [resource request response account id]
  (if-let [i (nth id (get-in @storage/instrumentations [account]))]
    (http/ret response i)
    (http/e404 response "Instrumentation not found.")))