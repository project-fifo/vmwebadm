(ns server.instrumentations.desc
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [dtrace :as dtrace]
            [server.http :as http]))


(def metrics-map
  {:module "module"
    :stat "stats"
    :label "label"
    :interval "interval"
    :fields "fields"
    :unit "unit"})

(defn handle [resource request response account]
  (http/write response 200
              {"Content-Type" "application/json"}
              (clj->json
               {:modules (dtrace/desc-modules)
                :fields (dtrace/desc-fields)
                :metrics (map #(transform-keys metrics-map %) (@dtrace/desc :metrics))
                :transformations {}})))