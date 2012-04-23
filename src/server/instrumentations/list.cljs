(ns server.instrumentations.list
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [server.http :as http]))

(def data-map {})

(defn handle [resource request response account]
  (http/ret response
            (map #(dissoc % :consumer) (get-in @storage/instrumentations [account]))))