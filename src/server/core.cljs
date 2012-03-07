(ns server.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:use [server.utils :only [clj->js]])
  (:require [server.routes :as routes]
            [cljs.nodejs :as node]))

(def http
  (node/require "http"))

(def url
  (node/require "url"))

(defn handler [req res]
  (routes/dispatch
   (vec (next (js->clj (.split (.-path (.parse url (. req -url))) "/"))))
   res))

(defn start [& _]
  (let [server (.createServer http handler)
        port 1337
        host "0.0.0.0"]
    (.listen server port host)
    (println "Server running at http://127.0.0.1:1337/")))

(set! *main-cli-fn* start)
