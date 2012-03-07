(ns server.routes
  (:require [server.vm :as vm])
  (:use [server.utils :only [clj->js]])
  (:use-macros [clojure.core.match.js :only [match]]))

(defn respond [content-type formater res content]
  (.writeHead res 200 (.-strobj {"Content-Type" content-type}))
  (.end res (formater content)))


(def res-text (partial respond "text/plain" str))
(def res-clj (partial
              respond
              "application/clj"
              pr-str))
(def res-json (partial
               respond
               "application/json"
               (fn [data] (.stringify js/JSON (clj->js data)))))

(def ext-map
  {"js" res-json
   "json" res-json
   "clj" res-clj})

(defn res-ext [ext]
  (get ext-map ext res-text))

(defn dispatch [resource res]
  (match [(:resource resource)]
         [[""]] (res-text res "root")
         [["vms"]] (vm/lookup
                    (fn [error resp]
                      ((res-ext (:ext resource)) res resp)))
         [["vms" uuid]] (vm/lookup
                         uuid
                         (fn [error resp]
                           ((res-ext (:ext resource)) res resp)))         
         [p]    (res-text res (str "Hello Path!\n" (pr-str resource)))))