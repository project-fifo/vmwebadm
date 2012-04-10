(ns dtrace.metrics.syscall
  (:require [dtrace :as dtrace]))


(def field
  {"syscall" {:type :str
              :name "probefunc"}
   "latency" {:type :int
              :name "this->latency"}})

(defn compile [zones
                       {decomposition "decomposition"
                        predicate "predicate"}]
  (let [pred (dtrace/compile-predicate field predicate)]
    (str
     "syscall:::entry"
     "/" (dtrace/compile-zone-predicate zones) "/"
     "{self->start[probefunc] = timestamp;}"
     "syscall:::return"
     "/"
     "self->start[probefunc]"
     (if predicate 
       (str "&&" pred)
       "")
     "/"
     "{"
     "this->latency=timestamp-self->start[probefunc];"
     (dtrace/compile-aggrs field decomposition)
     "}")))

(dtrace/register-metric
 ["syscall" "ops"]
 {:module "syscall"
  :stat "ops"
  :human-readable "System calls"
  :label "syscall operations"
  :interval "interval"
  :fields (keys field)
  :unit "operations"}
 compile)