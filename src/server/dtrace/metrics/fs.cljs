(ns dtrace.metrics.fs
  (:require [dtrace :as dtrace]))

(def field-logical
  {"optype" {:type :str
             :name "probefunc"}
   "latency" {:type :int
              :name "this->latency"}})

(def field-io
  {"optype" {:type :str
             :name "((probefunc == \"readch\") ? \"read\" : \"write\")"}
   "size" {:type :int
           :name "arg0"}})

(defn compile-logical [zones
                       {decomposition "decomposition"
                        predicate "predicate"}]
  (let [pred (dtrace/compile-predicate field-logical predicate)]
    (str
     "syscall::write:entry,"
     "syscall::read:entry"
     "/" (dtrace/compile-zone-predicate zones) "/"
     "{self->start[probefunc] = timestamp;}"
     "syscall::read:return,"
     "syscall::write:return"
     "/"
     "self->start[probefunc]"
     (if predicate 
       (str "&&" pred))
     "/"
     "{"
     "this->latency=timestamp-self->start[probefunc];"
     (dtrace/compile-aggrs (merge dtrace/default-fields field-logical) decomposition)
     "}")))

(defn compile-io [zones
                  {decomposition "decomposition"
                   predicate "predicate"}]
  (let [pred (dtrace/compile-predicate field-io predicate)]
    (str
     "sysinfo:::readch,"
     "sysinfo:::writech"
     "/" (dtrace/compile-zone-predicate zones) 
     (if predicate 
       (str "&&" pred))
     "/"
     "{"
     (dtrace/compile-aggrs field-io decomposition)
     "}")))

(dtrace/register-metric
 ["fs" "logical_ops"]
 {:module "fs"
  :stat "logical_ops"
  :human-readable "FS"
  :label "logical filesystem operations"
  :interval "interval"
  :fields (keys field-logical)
  :unit "operations"}
 compile-logical)

(dtrace/register-metric
 ["fs" "io_ops"]
 {:module "fs"
  :stat "io_ops"
  :human-readable "FS"
  :label "io related filesystem operations"
  :interval "interval"
  :fields (keys field-io)
  :unit "size"}
 compile-io)