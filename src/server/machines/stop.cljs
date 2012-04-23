(ns server.machines.stop
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn handle [resource request response uuid]
  (vm/stop
   uuid
   (fn [error _]
     (if error
       (http/e500 response (str error))
       (http/ret response "stopping.")))))