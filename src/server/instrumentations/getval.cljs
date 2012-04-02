(ns server.instrumentations.getval
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.storage :as storage]
            [server.http :as http]))

(defn handle [resource request response account id]
  (if-let [inst (nth (get-in @storage/data [:users account :instrumentations]) id)]
    (let [consumer (:consumer inst)
          data (:data inst)]
      (dtrace/aggwalk consumer
                      (fn [id k v]
                        (try
                          (swap! data assoc k v)
                          (catch js/Error e
                            (print "error:" (pr-str k) (pr-str v))))))
      (http/write response 200
                  {"Content-Type" "application/json"}
                  (try 
                    (clj->json
                     {:ok 
                      (vec @data)})
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