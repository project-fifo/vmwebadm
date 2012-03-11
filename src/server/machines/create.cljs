(ns server.machines.create
  (:use [server.utils :only [clj->js prn-js clj->json transform-keys prn]])
  (:require [server.vm :as vm]
            [server.http :as http]))

(defn- assoc-if [m m1 k]
  (if-let [v (m1 "metadata")]
                 (assoc m "metadata" v)
                 m))

(defn- build-spec [data]
  (if-let [spec 
           (if-let [package (data "package")]
             (if (= (first package) "{")
               (js->clj (.parse js/JSON package))))]
    (let [spec (assoc-if spec data "metadata")]
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
        spec))))

(defn handle [resource request response]
  (http/with-reqest-body request
    (fn [data]
      (if-let [spec (build-spec data)]
        (vm/create
         spec
         (fn [error vm]
           (if error
             (http/error response error)
             (http/ok response (.stringify js/JSON  vm)))))
        (http/error response (clj->json  {:error "Package not found"}))))))