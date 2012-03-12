(ns server.keys.add
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys]])
  (:require [server.storage :as storage]
            [cljs.nodejs :as node]
            [server.http :as http]))

(def fs (node/require "fs"))
(def cp (node/require "child_process"))

(defn handle [resource request response account]
  (http/with-reqest-body request
    (fn [data]
      (let [key (data "key")
            name (data "name")
            file (str "/tmp/tmp-key" account "-" name)
            cmd (str "ssh-keygen -f " file " -e -m pem")]
        (.writeFileSync fs file key)
        (print cmd "\n")
        (.exec cp
               cmd
               (fn [error, stdout, stderr]
                 (swap! storage/data assoc-in ["users" account "keys" (data "name")] stdout)
                 (storage/save)
                 (http/write response 200
                             {"Content-Type" "application/json"}
                             (clj->json data))))))))