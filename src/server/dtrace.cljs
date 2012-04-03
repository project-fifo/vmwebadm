(ns dtrace
  (:require
   [cljs.nodejs :as node]
   [clojure.string :as c.s])
  (:use
   [server.utils :only [prn]])
  (:use-macros
   [clojure.core.match.js :only [match]]))

(def libdtrace
  (node/require "node-libdtrace"))

(def consumer
  (.-Consumer libdtrace))

(def desc (atom
           {:metrics []}))
(def handler (atom {}))

(defn register-metric [p metric c]
  (swap! desc update-in [:metrics] conj metric)
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


(defn- get-deccomp [fmap name]
  (or
      (get-in fmap [name :decomposition])
      (get-in fmap [name :name])))(defn- get-deccomp [fmap name]
  (or
      (get-in fmap [name :decomposition])
      (get-in fmap [name :name])))

(defn compile-decomposition [field-map decomposition]
  (reduce
   (fn [s e]
     (str s "," e))
   (map
    (partial get-deccomp field-map)
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
   "zonename" {:type :str
               :name "zonename"}
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


(def aggr-funs
  {"count" 0
   "sum" 1
   "avg" 1
   "min" 1
   "max" 1
   "lquantize" 4
   "quantize" 1})






(defn- str-join [j es]
  (if (empty? es)
    ""
    (str (reduce
          (fn [s e]
            (str s j e))
          es))))


(declare compile-decomp)

(defn- math-fun [op fmap args]
  (str
   "("
   (str-join op (map (partial compile-decomp fmap) args))
   ")"))

(def funs
  {"*" (partial math-fun "*")
   "+" (partial math-fun "+")
   "/" (partial math-fun "/")
   "-" (partial math-fun "-")
   "str" #(str "\"" % "\"")
   "range" (fn [fmap [d r]]
             (pr d) (print "\n")
             (pr r) (print "\n")
             (let [c (compile-decomp fmap d)]
               (pr c) (print "\n")
               (str
                "("c"/"r")*"r",(("c"/"r")*"r")+" (dec r))))
   })

(defn- compile-decomp [fmap dcomp]
  (if (map? dcomp)
    (let [[f args] (first dcomp)]
      (if-let [f (funs f)]
        (f fmap args)))
    (if (number? dcomp)
      (str dcomp)
      (get-deccomp fmap dcomp))))

(defn- process-fields [fmap fields]
  (str-join "," (map
                 (partial compile-decomp fmap)
                 fields)))

(defn- compile-aggr [fmap aggr]
  (let [[name defs] (first aggr)
        [aggr-fn fields] (first defs)
        field-c (count fields)
        arg-cnt (aggr-funs aggr-fn)
        d-c (- field-c arg-cnt)
        aggr-fields (take d-c fields)]
    (if (re-matches #"^[a-zA-Z]+[a-zA-Z0-9]*$" name)
      (str
       "@" name
       (if (empty? aggr-fields)
         ""
         (str "["(process-fields fmap aggr-fields)"]"))
       "=" aggr-fn "("
       (process-fields fmap (drop d-c fields))
       ");")
      "")))
        
(defn compile-aggrs [fmap aggrs]
  (if (re-matches #"^[a-zA-Z]+[a-zA-Z0-9]*$" aggrs)
    (if-let [d (compile-decomp fmap aggrs)]
      (str "@=quantize("d");"))
    (apply str (map (partial compile-aggr fmap) (js->clj (.parse js/JSON aggrs))))))