(ns server.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:use [server.utils :only [clj->js]])
  (:require [server.routes :as routes]
            [clojure.string :as c.s]
            [cljs.nodejs :as node]))

(def http
  (node/require "http"))

(def url
  (node/require "url"))

(def path
  (node/require "path"))

(defn parse-url [req]
  (let [parts (vec (next (js->clj (.split (.-path (.parse url (. req -url))) "/"))))
        [base ext] (c.s/split (last parts) #"\.")
        resource (conj
                  (vec (butlast parts))
                  base)]
    {:parts parts
     :resource resource
     :method req.method
     :ext ext}))

(defn handler [req res]
  (routes/dispatch
   (parse-url req)
   req
   res))

(defn start [& _]
  (let [server (.createServer http handler)
        port 1337
        host "0.0.0.0"]
    (.listen server port host)
    (println "Server running at http://127.0.0.1:1337/")))

(set! *main-cli-fn* start)
