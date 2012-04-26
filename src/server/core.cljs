(ns server.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:use [server.utils :only [clj->js prn-js prn]])
  (:require [server.routes :as routes]
            [server.http :as http]
            [clojure.string :as c.s]
            [cljs.nodejs :as node]
            [server.storage :as storage]))

(def http
  (node/require "http"))

(def url
  (node/require "url"))

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
                :headers (js->clj (.-headers req))
                :query (if-let [qry (.-query url)]
                         (js->clj qry)
                         {})
                :ext ext}]
    return))

(defn handler [req res]h
  (routes/dispatch
   (parse-url req)
   req
   res))

(defn start [& _]
  (storage/init)
  (if (get-in @storage/data [:network :admin])
    (let [server (.createServer http handler)
          port (get-in @storage/data [:server :port] 80)
          host (get-in @storage/data [:server :host] "0.0.0.0")]
      (.listen server port host)
      (println "Server running at" (str "http://" host ":" port "/")))
    (println "Error: no admin network specified, please use ./client.sh to create it!")))

(set! *main-cli-fn* start)
