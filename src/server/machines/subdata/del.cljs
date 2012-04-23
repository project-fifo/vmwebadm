(ns server.machines.subdata.del
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))


(defn handle [key resource request response account uuid tag]
  (vm/update
   uuid
   {(str "remove_" key) [tag]}
   (fn [error resp]
     (if error
       (http/e500 response error)
       (http/ret response resp)))))