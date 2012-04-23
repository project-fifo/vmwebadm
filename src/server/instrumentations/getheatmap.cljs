(ns server.instrumentations.getheatmap
  (:use [server.utils :only [prn clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.storage :as storage]
            [server.http :as http]))


(defn- heatmapify-vals [vals]
  (reduce + (map second vals)))

(defn- grouping [res [[x _] _]]
  (let [x (mod x 1000)
        r (- x (mod x (/ 1000 res)))]
    r))

(defn- heatmapify [d res]
  (let [by-second (sort-by first (group-by #(floor (/ (ffirst  %) 1000)) d))
        summed (map
                (fn [[k vs]]
                  [k (sort-by
                      first
                      (map
                       (fn [[k vs]]
                         [k (reduce + (map second vs))])
                       (group-by (partial grouping res) vs)))]) by-second)]
    summed))

(defn handle [resource request response account id]
  (if-let [inst (nth (get-in @storage/instrumentations [account]) id)]
    (let [consumer (:consumer inst)]
      (try
        (http/ret response
                  {:value (heatmapify  (dtrace/values consumer) 10)
                   :transformations {}
                   :start_time 0
                   :duration 0})
        (catch js/Error e
          (http/e500 response
                     (str  "error during print:"
                           (pr-str @data))))))
    (http/e404 response
               "Instrumentation not found.")))