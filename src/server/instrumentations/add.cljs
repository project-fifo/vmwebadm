(ns server.instrumentations.add
  (:use [server.utils :only [clj->js clj->json]])
  (:require [server.storage :as storage]
            [cljs.nodejs :as node]
            [clojure.string :as c.s]
            [dtrace :as dtrace]
            [server.vm :as vm]
            [server.http :as http]))

(defn handle [resource request response account]
  (http/with-reqest-body request
    (fn [data]
      (if-let [clone (data "clone")]
        (let [clone (js/parseInt clone)]
          (swap! storage/data update-in [:users account :instrumentations]
                 #(conj % (nth % clone)))
          (http/write response 200
                      {"Content-Type" "application/json"}
                      (clj->json (get-in @storage/data [:users account :instrumentations clone]))))

        (if-let [handler (get-in @dtrace/handler [(data "module")
                                                   (data "stat")])]
          (let [data (if (data "predicate")
                       (assoc data "predicate" (js->clj
                                                (.parse
                                                 js/JSON
                                                 (data "predicate"))))
                       data)
                consumer (dtrace/new)
                data (assoc data :consumer consumer
                            :data (atom {}))]
            (vm/lookup
             {"owner_uuid" account}
             {:full false}
             (fn [error vms]
               (let [code (handler vms data)]
                 (print "=======DTRACE=======\n"
                        code
                        "====================\n"
                        "\n")
                 (dtrace/compile consumer code)
                 (dtrace/start consumer))))
            (swap! storage/data update-in [:users account :instrumentations]
                   #(conj % data))
            (http/write response 200
                        {"Content-Type" "application/json"}
                        (clj->json {:data data})))
          (http/write response 500
                      {"Content-Type" "application/json"}
                      (clj->json {:error "unknown metric"})))))))
