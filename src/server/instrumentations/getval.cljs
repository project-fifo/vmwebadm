(ns server.instrumentations.getval
  (:use [server.utils :only [prn clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.storage :as storage]
            [server.http :as http]))

(defn handle [resource request response account id]
  (if-let [inst (nth (get-in @storage/instrumentations [account]) id)]
    (let [consumer (:consumer inst)]
      (try 
      (http/ret response
                {:value (dtrace/values consumer)
                 :transformations {}
                 :start_time 0
                 :duration 0})
      (catch js/Error e
        (print "\n==========\n\n"  (.-message e) "\n" (.-stack e) "\n==========\n\n")
        (http/e500
         (str "error during print:"
              (pr-str @data))))))
    (http/e404 "not found")))