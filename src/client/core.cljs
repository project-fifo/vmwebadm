(ns client.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:require [cljs.nodejs :as node]))

(defn clj->js
  "Recursively transforms ClojureScript maps into Javascript objects,
   other ClojureScript colls into JavaScript arrays, and ClojureScript
   keywords into JavaScript strings."
  [x]
  (cond
   (string? x) x
   (keyword? x) (name x)
   (map? x) (.-strobj (reduce (fn [m [k v]]
                                (assoc m (clj->js k) (clj->js v))) {} x))
   (coll? x) (apply array (map clj->js x))
   :else x))

(defn clj->json [c]
  (.stringify js/JSON (clj->js c)))

(def fs (node/require "fs"))

(defn update-config [update-fn]
  (.writeFileSync fs "db.js" (clj->json (update-fn (js->clj (.parse js/JSON (.readFileSync fs "db.js"))))))
  )


(defn help []
  (print "Configuration tool\n"
                " passwd <user> <pass>   - adds a new user or resets a password for an existing one.\n"
                " port <port>            - sets the listen port for the server.\n"
                " host <host>            - sets the listen host for the server.\n"))
(defn start [& args]
  (pr args)
  (print "\n")
  (match [(vec args)]
         [["passwd" user passwd]]
         (update-config #(assoc-in % ["users" "users" user "passwd"] passwd))
         [["port" port]]
         (update-config #(assoc-in % ["server" "port"] (js/parseInt port)))
         [["host" host]]
         (update-config #(assoc-in % ["server" "host"] host))
         [["help"]]
         (help)
         [m]
         (do
           (pr m)
           (print "\n")
           (help))))

(set! *main-cli-fn* start)
