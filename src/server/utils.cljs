(ns server.utils
  (:require
   [cljs.nodejs :as node]
   [clojure.string :as c.s]))

(def crypto (node/require "crypto"))

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

(defn prn-js [json]
  (print (.stringify js/JSON json) "\n"))

(defn clj->json [c]
  (.stringify js/JSON (clj->js c)))

(defn transform-keys [key-map in]
  (reduce
   (fn [m [q-name d]]
     (if-let [val (in q-name)]
       (if (fn? d)
         (d m val)
         (assoc m d val))
       m))
   {}
   key-map))

(defn base64-decode [s]
  (->
   (js/Buffer. s "base64")
   (.toString "ascii")))

(defn prn [form]
  (print "prn>" (pr-str form) "\n"))


(defn hash-str [str]
  (-> ( .createHash crypto "sha512")
      (.update str)
      (.digest "base64")))

(defn nestify-map [map]
  (reduce
   (fn [m [k v]]
     (assoc-in m (c.s/split k #"\.") v))
   {}
   map))