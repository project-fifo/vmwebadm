(ns server.instrumentations.getval
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.storage :as storage]
            [server.http :as http]))

(defn- aggr-walker [data id k v]
  (try
    (swap! data assoc k v)
    (catch js/Error e
      (print "\n==========\n\n"  (.-message e) "\n" (.-stack e) "\n==========\n\n")
      (print "error:" (pr-str id) "-" (pr-str k) "-" (pr-str v)))))

(defn- prepare-results [data]
  (if (= (count (keys data)) 1)
    (second (first data))))

(defn handle [resource request response account id]
  (if-let [inst (nth (get-in @storage/data [:users account :instrumentations]) id)]
    (let [consumer (:consumer inst)
          data (:data inst)]
      (dtrace/aggwalk consumer (partial aggr-walker data))
      (http/write response 200
                  {"Content-Type" "application/json"}
                  (try 
                    (clj->json
                     {:value (prepare-results @data)
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