(ns server.instrumentations.add
  (:use [server.utils :only [clj->js clj->json]])
  (:require [server.storage :as storage]
            [cljs.nodejs :as node]
            [clojure.string :as c.s]
            [server.http :as http]))

(defn handle [resource request response account]
  (http/with-reqest-body request
    (fn [data]
      (if-let [clone (data "clone")]
        (let [clone (js/parseInt clone)]
          (swap! storage/data update-in [:users account :instrumentations]
                 #(conj % (nth % clone)))
          (http/write response 200
                      {"Content-Type" "application/json"}
                      (clj->json (get-in @storage/data [:users account :instrumentations clone]))))
        (do
          (swap! storage/data update-in [:users account :instrumentations]
                 #(conj % data))
          (http/write response 200
                      {"Content-Type" "application/json"}
                      (clj->json data)))))))
