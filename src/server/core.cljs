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

(def path
  (node/require "path"))

(def http-signature
  (node/require "http-signature"))  

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

(defn handler [req res]
  (try
    (let [parsed  (.parseRequest http-signature req)
          path (next (c.s/split (.-keyId parsed) #"/"))

          account (first path)
          path (concat ["users"] path)]
      
      (print (pr-str (js->clj parsed)) "\n"
             (pr-str path) "\n")
    
      (if-let [pub (get-in @storage/data path)]
        (if (.verifySignature http-signature parsed pub)
          (routes/dispatch
           (assoc
               (parse-url req)
             :account account)
           req
           res)
          (http/error res 401 "verification failed"))
        (http/error res 401 "key not found")))
    (catch js/Error e
      (try 
        (routes/dispatch
         (parse-url req)
         req
         res)
        (catch js/Error e
          (print "\n==========\n\n"  (.-message e) "\n" (.-stack e) "\n==========\n\n")
          (http/error res (http/encode-error "Error during not logged in dispatch." e)))))))

(defn start [& _]
  (storage/init)
  (let [server (.createServer http handler)
        port (get-in @storage/data ["server" "port"] 80)
        host (get-in @storage/data ["server" "host"] "0.0.0.0")]

    (.listen server port host)
    (println "Server running at" (str "http//" host ":" port "/"))))

(set! *main-cli-fn* start)
