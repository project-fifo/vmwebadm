(ns client.core
  (:use-macros [clojure.core.match.js :only [match]])
  (:require
   [cljs.reader :as reader]
   [clojure.string :as c.s]
   [cljs.nodejs :as node]))

(def fs (node/require "fs"))
(def crypto (node/require "crypto"))
(def util (node/require "util"))
(def cp
  (node/require "child_process"))


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
         " import package <package-file(s)> - imports one or more package files.\n"
         " default-dataset <dataset>        - sets the default dataset.\n"
         " passwd <user> <pass>             - adds a new user or resets a password for an existing one.\n"
         " list users                       - lists all known users\n"
         " promote <user>                   - grants a user admin rights.\n"
         " demote <user>                    - demotes a user from admin rights.\n"
         " delete <user>                    - deletes a user.\n"
         " port <port>                      - sets the listen port for the server.\n"
         " host <host>                      - sets the listen host for the server.\n"))

(defn import-pacakge [p]
  (let [p (read p)
        name (p :name)
        p (dissoc p :name)]
    (print "updating package:" (pr-str p) "\n")
    (update-config #(assoc-in % [:packages name] p))))

(defn format-users [[name u]]
  (print (.format util " [%s]  | [%s] | %s | %s\n"
                  (if (:admin u) "*" " ")
                  (if (:keys u) "*" " ")
                  (:uuid u) name)))

(defn list-users []
  (print "Admin | Key | UUID                                 | Login\n")
  (print "------+-----+--------------------------------------+------------------------\n")
  (doall
      (map
       format-users
       (:users (read "db.clj")))))

(defn passwd-user [user passwd]
  (.exec cp
         "uuid"
         (fn [error stdout stderr]
           (let [uuid (c.s/replace stdout #"\n" "")]
             (update-config
              #(if (get-in % [:users user :uuid])
                 (assoc-in % [:users user :passwd] (hash-str (str user ":" passwd)))
                 (assoc-in
                  (assoc-in % [:users user :passwd] (hash-str (str user ":" passwd)))
                  [:users user :uuid] uuid)))))))

(defn start [& args]
  (if (empty? args)
    (help)
    (match [(vec args)]
           [["import" "package" & pkgs]]
           (do
             (print "packages: " (pr-str pkgs) "\n")
             (doseq [pkg pkgs] (import-pacakge pkg)))
           [["default-dataset" dataset]]
           (update-config #(assoc-in % [:default-dataset] dataset))
           [["passwd" user passwd]]
           (passwd-user user passwd)
           [["promote" user]]
           (update-config #(if (get-in % [:users user])
                             (assoc-in  % [:users user :admin] true)
                             (do
                               (print "Unknown user" (str user ".\n"))
                               %)))
           [["demote" user]]
           (update-config #(if (get-in % [:users user])
                             (assoc-in  % [:users user :admin] false)
                             (do
                               (print "Unknown user" (str user ".\n"))
                               %)))
           [["delete" user]]
           (update-config #(update-in  % [:users] (fn [m] (dissoc m user))))
           [["list" "users"]]
           (list-users)
           [["port" port]]
           (update-config #(assoc-in % [:server :port] (js/parseInt port)))
           [["host" host]]
           (update-config #(assoc-in % [:server :host] host))
           [["help"]]
           (help)
           :else
           (do
             (print "Unknown command: "
                    (pr m))
             (print "\n")
             (help)))))

(set! *main-cli-fn* start)