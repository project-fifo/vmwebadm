(ns server.vm
  (:use [server.utils :only [clj->js]])
  (:require [cljs.nodejs :as node]))

(defn wrap-callback [callback]
  (fn [error result]
    (callback (js->clj error) (js->clj result))))

(def VM
  (node/require "VM"))

(defn start [uuid callback]
  (.start
   VM
   (clj->js uuid)
   (wrap-callback callback)))

(defn stop [uuid callback]
  (.stop
   VM
   (clj->js uuid)
   (wrap-callback callback)))

(defn info [uuid callback]
  (.info
   VM
   (clj->js uuid)
   (clj->js ["all"])
   (wrap-callback callback)))

(defn lookup
  ([callback]
     (.lookup
      VM
      (clj->js {})
      (clj->js {:full, true})
      (wrap-callback callback)))
  ([search callback]
     (.lookup
      VM
      (clj->js
       (if (string? search)
         {"zonename" search}
         search))
      (clj->js {:full, true})
      (wrap-callback callback))))