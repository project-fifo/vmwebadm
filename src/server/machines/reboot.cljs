(ns server.machines.reboot
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn handle [resource request response uuid]
  (vm/reboot
   uuid
   (fn [error _]
     (if error
       (http/e500 response (str error))
       (http/ret response "Rebooting.")))))