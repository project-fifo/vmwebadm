(ns server.machines.create
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn log]])
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

(defn- kvm? [spec]
  (= "kvm" (spec :brand)))

(defn- add-network [dataste spec]
  (log 4 "add-network")
  [admin (if (get-in @storage/data [:network :admin])
           (storage/next-ip :admin)
           false)
   ext (if (get-in @storage/data [:network :ext])
           (storage/next-ip :ext)
           false)
   ext   (if ext
           (assoc ext "primary" true))
   admin (if (and admin (not ext))
           (assoc admin "primary" true)
           admin)
   specs (if ext
           (update-in spec ["nics"] conj ext)
           specs)
   specs (if admin
           (update-in spec ["nics"] conj admin)
           specs)
   specs (if (and (not admin) (not ext))
           (update-in spec ["nics"] conj {"nic_tag" "admin"
                                          "ip" "dhcp"
                                          "primary" true})
           specs)]
  spec)

(defn- set-driver [dataset type spec]
  (log 4 "set-driver: " type)
  (if-let [model (dataset type)]
    (assoc spec type model)
    spec))

(defn- set-alias [data spec]
  (log 4 "set-alias")
  (if-let [alias (data "name")]
    (assoc spec
      "alias" alias)
    spec))

(defn- set-basics [dataset data spec]
  (log 4 "set-basic")
  (assoc
      (assoc-if spec data "metadata" "customer_metadata")
    :brand (get-brand (dataset "os"))
    :resolvers (get @storage/data :resolvers ["8.8.8.8" "4.4.4.4"])))

(defn- set-memory [spec]
  (log 4 "set-memory")
  (if (kvm? spec)
    (let [ram (spec :max_physical_memory)]
      (assoc spec
        :ram ram
        :max_physical_memory (+ ram 1024)))
    spec))

(defn set-dataset [dataset spec]
  (log :debug "set-dataset: " dataset)
  (let [uuid (dataset "uuid")]
    (if (kvm? spec)
      (let [[f & r] (spec :disks)]
        (assoc spec :disks (concat [(assoc f "image_uuid" uuid)] r)))
      (assoc spec "dataset_uuid" uuid))))

(defn init-spec [data]
  (log 4 "init-spec")
  (if-let [package (data "package")]
             (if (= (first package) "{")
               (js->clj (.parse js/JSON package))
               (get-in @storage/data [:packages package]))
             (get @storage/data :default-dataset)))

(defn set-disks [spec]
  (log 4 "set-disks")
  (if (kvm? spec)
    (if (and (spec :quota) (empty? (spec :disks)))
      (assoc spec :disks
             [{"size" (* (spec :quota) 1024)
               "boot" true}])
      spec)
    spec))

(defn- build-spec [data dataset]
  (let [spec (->> (init-spec data)
                  (set-basics dataset data)
                  (set-alias data)
                  (set-disks)
                  (set-memory)
                  (set-dataset dataset)
                  (set-driver dataset "disk_driver")
                  (set-driver dataset "nic_driver")
                  (add-network dataset))]
    (log 5 "spec: " spec)
    spec))

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