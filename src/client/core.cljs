(ns client.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:require
   [cljs.reader :as reader]
   [cljs.nodejs :as node]))

(def fs (node/require "fs"))
(def crypto (node/require "crypto"))

(defn slurp [f]
  (str (.readFileSync fs f)))

(defn read [f]
  (reader/read-string (slurp f)))

(defn update-config [update-fn]
  (.writeFileSync fs "db.clj" (pr-str (update-fn (read "db.clj")))))

(defn hash-str [str]
  (-> ( .createHash crypto "sha512")
      (.update str)
      (.digest "base64")))

(defn help []
  (print "Configuration tool\n"
         " import pacakge <package-file(s)> - imports one or more pacakge files.\n"
         " passwd <user> <pass>             - adds a new user or resets a password for an existing one.\n"
         " port <port>                      - sets the listen port for the server.\n"
         " host <host>                      - sets the listen host for the server.\n"))

(defn import-pacakge [p]
  (print p ":" (slurp p) "\n")
  
  (let [p (read p)
        name (p :name)
        p (dissoc p :name)]
    (print "updating package:" (pr-str p) "\n")
    (update-config #(assoc-in % [:packages name] p))))

(defn start [& args]
  (match [(vec args)]
         [["import" "package" & pkgs]]
         (do
           (print "packages: " (pr-str pkgs) "/" (pr-str (vec args)) "\n")
           (doseq [pkg pkgs] (import-pacakge pkg)))
         [["passwd" user passwd]]
         (update-config #(assoc-in % [:users user :passwd] (hash-str (str user ":" passwd))))
         [["port" port]]
         (update-config #(assoc-in % [:server :port] (js/parseInt port)))
         [["host" host]]
         (update-config #(assoc-in % [:server :host] host))
         [["help"]]
         (help)
         [m]
         (do
           (pr m)
           (print "\n")
           (help))))

(set! *main-cli-fn* start)
