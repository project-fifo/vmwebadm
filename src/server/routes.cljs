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

(defn dispatch [resource request response]
  (let [ext (:ext resource)
        method (:method resource)
        path (:resource resource)
        out (partial (res-ext ext) response)
        default-callback (fn [error resp]
                           (if error
                             (out error)
                             (out resp)))]
    (match [method path]
           [_ [""]]
           (response-text response "root")
           ["GET" ["vms"]]
           (vm/lookup default-callback)
           ["GET" ["vms" uuid]]
           (vm/lookup uuid default-callback)
           ["DELETE" ["vms" uuid]]
           (vm/delete uuid default-callback)
           ["PUT" ["vms" uuid]]
           (let [body (atom "")]
             (print "put:" uuid "\n")
             (.on request "data"
                  (fn [cache]
                    (prn cache)
                    (print " <cache\n")
                    (swap! body str cache)))
             (.on request "end"
                  #(vm/update
                    uuid
                    (js->clj (.parse js/JSON @body))
                    default-callback)))
           
           [_ p]    (response-text response (str "Hello Path!\n" (pr-str responseource))))))