(ns server.machines.start
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn log]])
  (:require [server.vm :as vm]
            [cljs.nodejs :as node]
            [server.http :as http]))

(def fs
  (node/require "fs"))

(def utils
  (node/require "util"))

(defn startvm [response uuid cdrom]
  (log :info "starting vm: " uuid " with" (if (not cdrom) "out") " cdrom.")
  (vm/start
   uuid
   (if cdrom
     {:disks
      [{:media "cdrom"
        :path "/boot.iso"
        :model "ide"}]
      :boot "order=cd,once=d"
      }
     {})
   (fn [error]
     (if error
       (http/e500 response (str error))
       (http/ret response "starting")))))

(defn handle [resource request response uuid]
  (http/with-reqest-body request
    (fn [data]
      (if-let [image (data "image")]
        (vm/lookup
         {"uuid" uuid}
         {:full true}
         (fn [error vms]
           (if error
             (http/error response error)
             (if-let [vm (first vms)]
               (let [iso (str image ".iso")
                     from-file (str "data/images/" iso)
                     to-file (str (vm "zonepath") "/root/boot.iso")
                     oldFile (.createReadStream fs from-file)
                     newFile (.createWriteStream fs to-file)]
                 (log :info "copying " from-file " -> " to-file)
                 (.pump utils oldFile newFile
                        (fn [e]
                          (if e
                            (do
                              (log :error e))
                            (startvm response uuid true))))
                 )
               (http/e404 response "VM Not founds.")))))

        (startvm response uuid false)))))