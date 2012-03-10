(ns server.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:use [server.utils :only [clj->js prn-js prn]])
  (:require [server.routes :as routes]
            [clojure.string :as c.s]
            [cljs.nodejs :as node]))

(def *debug* true)
(def http
  (node/require "http"))

(def url
  (node/require "url"))

(def path
  (node/require "path"))

(defn parse-url [req]
  (let [url (.parse url (.-url req) true)
        parts (vec (next (js->clj (.split (.-pathname url) "/"))))
        [base ext] (c.s/split (last parts) #"\.")
        resource (conj
                  (vec (butlast parts))
                  base)
        return {:parts parts
                :resource resource
                :method (.-method req)
                :query (if-let [qry (.-query url)]
                         (js->clj qry)
                         {})
                :ext ext}]
    return))

(defn handler [req res]
  (if *debug*
    (print "url:" (.-url req)
           "method:" (.-method req)
           "query:" (.-query req)))
  (routes/dispatch
   (parse-url req)
   req
   res))

(defn start [& _]
  (let [server (.createServer http handler)
        port 80
        host "0.0.0.0"]
    (.listen server port host)
    (println "Server running at http://127.0.0.1:1337/")))

(set! *main-cli-fn* start)
