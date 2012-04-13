(ns dtrace.metrics.erlang
  (:require [dtrace :as dtrace]))


(def probefunc-map
  {"exit" "process-exit"
   "spawn" "process-spawn"
   "scheduled" "process-scheduled"
   "unscheduled" "process-unscheduled"
   "hibernate" "process-hibernate"
   "heapgrow" "process-heap_grow"
   "heapshrink" "process-heap_shrink"})

(def fields-process
  {"optype" {:type :enum
             :enum (set (keys probefunc-map))
             :name (dtrace/transform-str "probename" probefunc-map)
             :decomposition {"" {"count" ["milisecond"]}}}})

(defn compile-process [zones
                       {decomposition "decomposition"
                        predicate "predicate"}]
  (let [pred (dtrace/compile-predicate dtrace/default-fields predicate)]
    (str
     "erlang*:::process-*"
     "/" (dtrace/compile-zone-predicate zones)
     (if predicate
       (str "&&" (dtrace/compile-predicate fields-process predicate)))
     "/"
     "{"
     (dtrace/compile-aggrs fields-process decomposition)
     "}")))

(dtrace/register-metric
 ["erlang" "process"]
 {:module "erlang"
  :stat "process"
  :human-readable "Processes"
  :label "erlang process opperations"
  :interval "interval"
  :fields (keys fields-process)
  :unit "operations"}
 compile-process)