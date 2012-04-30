(ns server.machines.create
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [cljs.nodejs :as node]
            [server.http :as http]
            [server.storage :as storage]))


(def dsadm
  (node/require "dsadm"))

(defn- assoc-if [m m1 from-k to-k]
  (if-let [v (m1 from-k)]
    (assoc m to-k v)
    m))

(defn- get-brand [os]
  (get {"smartos" "joyent"}
       os
       "kvm"))

(defn- build-spec [data dataset]
  (if-let [spec 
           (if-let [package (data "package")]
             (if (= (first package) "{")
               (js->clj (.parse js/JSON package))
               (get-in @storage/data [:packages package]))
             (get @storage/data :default-dataset))]
    (let [spec (let [spec (assoc
                              (assoc-if spec data "metadata" "customer_metadata")
                            :brand (get-brand (dataset "os")))]
                 (if-let [dataset  (data "dataset")]
                   (if (= (spec :brand) "kvm")
                     (let [spec (if (and (spec :quota) (empty? (spec :disks)))
                                  (assoc spec :disks
                                         [{"size" (* (spec :quota) 1024)
                                           "boot" true}])
                                  spec)

                           ram (get spec :max_physical_memory)
                           spec (assoc spec "ram" ram
                                       :max_physical_memory (+ ram 1024))]
                       (if-let [disks (spec :disks)]
                         (let [f (first disks)
                               r (rest disks)
                               disks
                               (concat [(assoc f "image_uuid" dataset)] r)]
                           (assoc spec :disks disks))
                         spec))
                     (assoc spec "dataset_uuid" dataset))
                   spec))
          spec (assoc
                   (if-let [alias (data "name")]
                     (assoc spec
                       "alias" alias)
                     spec)
                 "nics"  [(assoc (storage/next-ip :admin)
                            "primary" true)])
          spec (if-let [model (dataset "disk_driver")]
                 (assoc spec "disk_driver" model)
                 spec)
          spec (if-let [model (dataset "nic_driver")]
                 (assoc spec "nic_driver" model)
                 spec)]
      (if (get-in @storage/data [:network :ext])
        (update-in spec ["nics"] conj (storage/next-ip :ext))
        spec)

      )))

(defn add-keys [login data]
  (assoc-in data ["customer_metadata" "root_authorized_keys"]
            (apply str (map (fn [[k v]] (:ssh v))
                             (get-in @storage/data [:users login :keys])))))

(defn handle [resource request response login]
  (http/with-reqest-body request
    (fn [data]
      (.show
       dsadm
       (data "dataset")
       (fn [error dataset]
         (let [dataset (js->clj dataset)]
           (if-let [spec (build-spec data dataset)]
             (let [spec (add-keys login spec)]
               (vm/create
                (assoc spec "owner_uuid" (get-in @storage/data [:users login :uuid]))
                (fn [error vm]
                  (if error
                    (http/e500 response (str  "Error in server.machien.create: "  (pr-str (js->clj error))))
                    (http/ret response (assoc vm "zonename" (or (spec "alias") (vm "zonename"))))))))
             (http/e404 response "Package not found"))))))))