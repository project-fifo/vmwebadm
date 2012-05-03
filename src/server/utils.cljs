(ns server.utils
  (:require
   [cljs.nodejs :as node]
   [server.storage :as storage]
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

(def log-syms
  {:error 1
   :warning 2
   :info 3
   :debug 4
   :trace 5})

(defn- lvl-to-str [lvl]
  (if-let [e (get [" ALL "
                   " ERR "
                   "WARN "
                   "INFO "
                   " DBG "
                   "TRACE"] lvl)]
    e
    (str "LVL" lvl)))

(defn log [lvl & strs]
  (let [lvl (if (number? lvl)
              lvl
              (log-syms lvl))]
    (if (>= (get @storage/data :debug 0) lvl)
      (print (str "[" (lvl-to-str lvl) "]") (apply str (map #(if (string? %) % (pr-str %)) strs)) "\n"))))


(defn log-exception [strs]
  (apply log :error strs))

(defn log-error [strs]
  (apply log :error strs))

(defn transform-keys [key-map in]
  (reduce
   (fn [m [q-name d]]
     (if-let [val (in q-name)]
       (if (fn? d)
         (if-let [r (d m val)]
           r
           m)
         (assoc m d val))
       m))
   {}
   key-map))

(defn base64-decode [s]
  (->
   (js/Buffer. s "base64")
   (.toString "ascii")))

(defn prn [& forms]
  (print "prn>" (reduce
                 #(str %1 " " %2)
                 (map #(pr-str %) forms)) "\n"))


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
