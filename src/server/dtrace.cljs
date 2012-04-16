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
  (atom {:consumer
         (new consumer)
         :data (atom {})}))

(defn compile [c code]
  (.strcompile (:consumer @c) code)
  c)

(defn setopt [c opt val]
  (.setopt (:consumer @c) opt val)
  c)

(defn consume [c f]
  (.consume (:consumer @c)
            (fn [p r]
              (f (js->clj p) (js->clj r))))
  c)

(defn update-map [data vs]
  (reduce
   (fn [m [k v]]
     (update-in m
                [k]
                (fn [b] (+ (or b 0) v))))
   data
   vs))

(defn- aggr-walker [data id k v]
  (try
    (if (seq? v)
      (swap! data update-map v)
      (swap! data assoc k v))
    (catch js/Error e
      (print "\n==========\n\n"  (.-message e) "\n" (.-stack e) "\n==========\n\n")
      (print "error:" (pr-str id) "-" (pr-str k) "-" (pr-str v)))))

(defn aggwalk [c f]
  (.aggwalk (:consumer @c)
            (fn [i k v]
              (f (js->clj i)
                 (js->clj k)
                 (js->clj v))))
  c)

(defn update [c]
  (aggwalk
   c
   (fn [i k v]
     (aggr-walker (:data @c) i k v))))

(defn values [c]
  (update c)
  (vec @(:data @c)))

(defn stop [c]
  (.stop (:consumer @c))
  (if-let [timer (:timer @c)]
    (do
      (js/clearInterval timer)
      (swap! c dissoc :timer)))
  c)

(defn start [c]
  (.go (:consumer @c))
  
  (if (not (:timer @c))
    (swap! c assoc :timer
           (js/setInterval
            (fn []
              (update c))
            1000)))
  c)


(defn- get-deccomp [fmap name]
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


(defn transform-str [name m]
  (reduce
   (fn [s v]
     (str "((" name "==" "\"" (m v) "\")?" "\"" v "\":" s ")"))
   (str "\"unknown\"")
   (keys m)))

(def default-fields
  {"pid" {:type :int
          :name "pid"}
   "second" {:type :int
             :name "(timestamp/1000000000)"}
   "milisecond" {:type :int
                 :name "(timestamp/1000000)"}
   "submilisecond" {:type :int
                    :name "((timestamp/1000000)%1000)"}
   "subsecondmap"  {:type :int
                    :name "(timestamp/1000000000),((timestamp/1000000)%1000)"}
   "execname" {:type :str
               :name "execname"}
   "probefunc" {:type :str
                :name "probefunc"}
   "probemod" {:type :str
               :name "probemod"}
   "probename" {:type :str
                :name "probename"}
   "timestamp" {:type :int
                :name "timestamp"}
   "zonename" {:type :str
               :name "zonename"}
   "probeprov" {:type :str
                :name "probeprov"}})

(defn desc-fields []
  (reduce
   (fn [m k]
     (assoc m k (get-in default-fields [m :desc] {:desc "global instrumentation field"})))
   {}
   (keys default-fields)))

(defn desc-modules []
  (reduce
   (fn [m k]
     (assoc
         m
       (:module k)
       (:human-readable k)))
   {}
   (:metrics @desc)))

(def pred-map
  {"eq" "=="
   "gt" ">"
   "gte" ">="
   "lt" "<"
   "lte" "<="
   "neq" "!="})

(def converter-map
  {:str  #(str "\"" %1 "\"")
   :enum (fn [x m]
           (if ((:enum  m) x)
             (str "\"" x "\"")
             (throw (str "invalid enum value '" x "' valid choices are: " m))))
   :int (fn [x y] x)})

(defn converter [t d m]
  ((if (fn? t)
     t
     (converter-map t))
   d m))

(defn compile-zone-predicate [zones]
  (if (= zones :all)
    "(1==1)"
    (str
     "("
     (reduce
      (fn [s e]
        (str  s "||" e ))
      (map
       #(str "zonename==\"" % "\"")
       zones))
     ")")))

(defn compile-predicate* [field-map predicate]
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
         (let [type-map  (field-map a)]
           (if-let [{type :type
                     name :name} type-map]
             (do
               (str "(" name (pred-map pred) (converter type b type-map) ")"))))))

(defn compile-predicate [fmap predicate]
  (compile-predicate* (merge default-fields fmap) predicate))

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
             (let [c (compile-decomp fmap d)]
               (str
                "("c"/"r")*"r",(("c"/"r")*"r")+" (dec r))))})

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
    (if (re-matches #"^[a-zA-Z]*[a-zA-Z0-9]*$" name)
      (str
       "@" name
       (if (empty? aggr-fields)
         ""
         (str "["(process-fields fmap aggr-fields)"]"))
       "=" aggr-fn "("
       (process-fields fmap (drop d-c fields))
       ");")
      "")))

(defn- compile-aggrs* [fmap aggrs]
  (if (re-matches #"^[a-zA-Z]+[a-zA-Z0-9]*$" aggrs)
    (if-let [d (compile-decomp fmap aggrs)]
      (if (string? d)
        (str "@=quantize("d");")
        (do
          (pr d) (print "<-d\n")
          (pr fmap) (print "<-fmap\n")
          (pr (compile-aggr fmap d)) (print "<-res\n")
          (compile-aggr fmap d))))
    (apply
     str
     (map
      (partial compile-aggr fmap)
      (js->clj (.parse js/JSON aggrs))))))

(defn compile-aggrs [fmap aggrs]
  (compile-aggrs* (merge default-fields fmap) aggrs))