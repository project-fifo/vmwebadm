(ns dtrace
  (:require
   [cljs.nodejs :as node])
  (:use-macros
   [clojure.core.match.js :only [match]]))

(def libdtrace
  (node/require "node-libdtrace"))

(def consumer
  (.-Consumer libdtrace))

(def desc (atom
           {:metrics []}))
(def handler (atom {}))

(defn register-metric [p d c]
  (swap! desc update-in [:metrics] conj m)
  (swap! handler assoc-in p c))

(defn new []
  (new consumer))

(defn compile [c code]
  (.strcompile c code)
  c)

(defn setopt [c opt val]
  (.setopt c opt val)
  c)

(defn consume [c f]
  (.consume c
            (fn [p r]
              (f (js->clj p) (js->clj r))))
  c)

(defn aggwalk [c f]
  (.aggwalk c (fn [i k v]
                (f (js->clj i) (js->clj k) (js->clj v))))
  c)

(defn stop [c]
  (.stop c)
  c)

(defn start [c]
  (.go c)
  c)


(defn compile-decomposition [field-map decomposition]
  (reduce
   (fn [s e]
     (str s "," e))
   (map
    #(or
      (get-in field-map [% :decomposition])
      (get-in field-map [% :name]))
    (if (seq? decomposition)
      decomposition
      [decomposition]))))

(def default-fields
  {"pid" {:type :int
          :name "pid"}
   "execname" {:type :str
               :name "execname"}
   "probefunc" {:type :str
                :name "probefunc"}
   "probemod" {:type :str
               :name "probemod"}
   "probename" {:type :str
                :name "probename"}
   "probeprov" {:type :str
                :name "probeprov"}})

(def pred-map
  {"eq" "=="
   "gt" ">"
   "gte" ">="
   "lt" "<"
   "lte" "<="
   "neq" "!="})

(def converter-map
  {:str  #(str "\"" % "\"")
   :int (fn [x] x)})

(defn converter [t d]
  ((if (fn? t)
     t
     (converter-map t))
   d))


(defn compile-zone-predicate [zones]
  (str
   "("
   (reduce
    (fn [s e]
      (str  s "||" e ))
    (map
     #(str "zonename==\"" % "\"")
     zones))
   ")"))

(defn compile-predicate [field-map predicate]
  (match [(first predicate)]
         [[:and preds]]
         (str "("
              (reduce
               (fn [s p]
                 (str s " && "p))
               (map compile-predicate preds))
              ")")
         [[:or preds]]
         (str "("
              (reduce
               (fn [s p]
                 (str s " && "p))
               (map compile-predicate preds))
              ")")
         [[pred [a b]]]
         (if-let [{type :type
                   name :name} (field-map a)]
           (do
             (str "(" name (pred-map pred) (converter type b) ")")))))
