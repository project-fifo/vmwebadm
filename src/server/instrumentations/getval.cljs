(ns server.instrumentations.getval
  (:use [server.utils :only [prn clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.storage :as storage]
            [server.http :as http]))

(defn handle [resource request response account id]
  (if-let [inst (nth (get-in @storage/instrumentations [account]) id)]
    (let [consumer (:consumer inst)]
      (http/write response 200
                  {"Content-Type" "application/json"}
                  (try 
                    (clj->json
                     {:value (dtrace/values consumer)
                      :transformations {}
                      :start_time 0
                      :duration 0})
                    (catch js/Error e
                      (print "\n==========\n\n"  (.-message e) "\n" (.-stack e) "\n==========\n\n")
                      (clj->json
                       {:error
                        ["error during print:"
                         (pr-str @data)]})))))
    (http/write response 404
                {"Content-Type" "application/json"}
                (clj->json
                 {:error "not found"}))))