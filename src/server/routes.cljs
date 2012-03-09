(ns server.routes
  (:require [server.vm :as vm])
  (:use [server.utils :only [clj->js prn-js clj->json]])
  (:use-macros [clojure.core.match.js :only [match]]))

(defn write [response code headers content]
  (.writeHead response code (clj->js headers))
  (.end response content))

(defn respond [content-type formater res content]
  (write res 200 {"Content-Type" content-type}
         (formater (or content ""))))


(def res-text (partial respond "text/plain" str))
(def res-clj (partial
              respond
              "application/clj"
              pr-str))
(def res-json (partial
               respond
               "application/json"
               (fn [data] (.stringify js/JSON (clj->js data)))))

(def ext-map
  {"js" res-json
   "json" res-json
   "clj" res-clj})

(defn res-ext [ext]
  (get ext-map ext res-json))

(defn with-reqest-body [request response callback]
  (let [body (atom "")]
    (.on request "data"
         (fn [data]
           (swap! body str data)))
    (.on request "end"
         (fn []
           (let [r (if (= @body "")
                     {}
                     (js->clj (.parse js/JSON @body)))]
             (callback r ))))))


(defn dispatch [resource request response]
  (let [ext (:ext resource)
        method (:method resource)
        path (:resource resource)
        qry (:query resource)
        out (partial (res-ext ext) response)
        default-callback (fn [error resp]
                           (if error
                             (out error)
                             (out resp)))]
    (match [method path]
           [_ [""]]
           (response-text response "root")
           ["GET" ["machines"]]
           (let [q (dissoc qry "limit" "offset")]
             (vm/lookup q
                        (fn [error vms]
                          (if error
                            (default-callback error vms)
                            (let [limit (qry "limit")
                                  offset (get qry "offset" 0)
                                  vms (drop offset vms)
                                  vms (if limit (take limit vms) vms)
                                  cnt (count vms)]
                              (write response 200
                                     {"Content-Type" "application/json"
                                      "x-resource-count" cnt
                                      "x-query-limit" (or limit (inc cnt))}
                                     (clj->json vms)))))))
           ["PUT" ["vms"]]
           (with-reqest-body request response
             (fn [data]
               (vm/create
                data
                default-callback)))           
           ["GET" ["vms" uuid]]
           (vm/lookup uuid default-callback)
           ["DELETE" ["vms" uuid]]
           (vm/delete uuid default-callback)
           ["POST" ["vms" uuid]]
           (with-reqest-body request response
             (fn [data]
               (condp = (data "state")
                 "off" (vm/start uuid (fn []))
                 "started" (vm/stop uuid (fn [])))
               (vm/update
                uuid
                (dissoc data "state")
                default-callback)))
           [_ p]    (response-text response (str "Uhh can't find that" (pr-str responseource))))))