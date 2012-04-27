(ns server.machines.create
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]
            [server.storage :as storage]))



(defn- assoc-if [m m1 from-k to-k]
  (if-let [v (m1 from-k)]
    (assoc m to-k v)
    m))

(defn- build-spec [data]
  (if-let [spec 
           (if-let [package (data "package")]
             (if (= (first package) "{")
               (js->clj (.parse js/JSON package))
               (get-in @storage/data [:packages package]))
             (get @storage/data :default-dataset))]
    (let [spec (let [spec (assoc-if spec data "metadata" "customer_metadata")]
                 (if-let [dataset  (data "dataset")]
                   (if (= (spec "brand") "kvm")
                     (if-let [disks (spec "disks")]
                       (let [f (first disks)
                             r (rest disks)
                             disks
                             (concat [(assoc f "image_uuid" dataset)] r)]
                         (assoc spec "disks" disks))
                       spec)
                     (assoc spec "dataset_uuid" dataset))
                   spec))
          spec (assoc
                   (if-let [alias (data "name")]
                     (assoc spec
                       "alias" alias)
                     spec)
                 "nics"  [(assoc (storage/next-ip :admin)
                            "primary" true)])]
      (if (get-in @storage/data [:network :ext])
        (update-in spec ["nics"] conj (storage/next-ip :ext))
        spec))))

(defn add-keys [login data]
  (assoc-in data ["customer_metadata" "root_authorized_keys"]
            (map (fn [[k v]] (:ssh v))
                 (get-in @storage/data [:users login :keys]))))

(defn handle [resource request response login]
  (http/with-reqest-body request
    (fn [data]
      (if-let [spec (build-spec data)]
        (let [spec (add-keys login spec)]
          (vm/create
           (assoc spec "owner_uuid" (get-in @storage/data [:users login :uuid]))
           (fn [error vm]
             (if error
               (http/e500 response (str  "Error in server.machien.create: "  (pr-str (js->clj error))))
               (http/ret response (assoc vm "zonename" (or (spec "alias") (vm "zonename"))))))))
        (http/e404 response "Package not found")))))