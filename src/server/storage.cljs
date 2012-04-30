(ns server.storage
  (:require [cljs.nodejs :as node]
            [cljs.reader :as reader]))

(defn- bit [x n] 
  (bit-and (bit-shift-right x (* n 8)) 0xFF))

(defn- to-bytes [x]
  [(bit x 3) (bit x 2) (bit x 1) (bit x 0)])

(defn- to-ip [x]
  (let [[a b c d] (to-bytes x)]
    (str a "." b "." c "." d)))

(defn- from-bytes [a b c d]
  (+ (bit-shift-left a 24)
     (bit-shift-left b 16)
     (bit-shift-left c 8)
     d))

(defn- from-ip [ip]
  (apply from-bytes (map #(js/parseInt %) (rest (re-matches #"(\d+)\.(\d+)\.(\d+)\.(\d+)" ip)))))


(def instrumentations (atom {}))

(def data (atom {}))

(def fs (node/require "fs"))

(defn- slurp [file]
  (str (.readFileSync fs file)))

(defn save []
  (.writeFileSync fs "db.clj" (pr-str @data)))



(defn reclaim-ip [type ip]
  (init)
  (let [ip (from-ip ip)
        first (get-in @data [:network type :first])]
    (if (= (dec first) ip)
      (swap! data update-in [:network type :first] dec)
      (swap! data update-in [:network type :free] conj ip)))
  (save))

(defn- get-ip [type]
  (init)
  (let [res 
        (if-let [ip (first (get-in @data [:network type :free]))]
          (do
            (swap! data update-in [:network type :free] rest)
            ip)
          (let [ip (get-in @data [:network type :first])
                gw (get-in @data [:network type :gw])]
            (swap! data update-in [:network type :first] inc)
            (if (= gw ip)
              (do
                (swap! data update-in [:network type :first] inc)
                (inc ip))
              ip)))]
    (save)
    res))

(defn next-ip [type]
  (let [ip (get-ip type)
        gw (get-in @data [:network type :gw])
        mask (get-in @data [:network type :mask])]
    {"nic_tag" ({:admin "admin"
                 :ext "external"} type)
     "ip" (to-ip ip)
     "netmask" (to-ip mask)
     "gateway" (to-ip gw)}))

(defn init []
  (reset! data (js->clj (reader/read-string (slurp "db.clj")))))

(js/setInterval
 init (* 30 1000))
