(ns server.machines.subdata.add
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn handle [key resource request response account uuid]
  (http/with-reqest-body request
    (fn [data]
      (vm/update
       uuid
       {(str "set_" key) data}
       (fn [error resp]
         (if error
           (http/e500 response error)
           (http/ret response resp)))))))