(ns server.routes
  (:require [server.vm :as vm])
  (:use-macros [clojure.core.match.js :only [match]]))

(defn res-text [res text]
  (.writeHead res 200 (.-strobj {"Content-Type" "text/plain"}))
  (.end res text))

(defn dispatch [path res]
  (match [path]
         [[""]] (vm/lookup (fn [error resp]
                             (res-text res (pr-str resp))))
         [p]    (res-text res (str "Hello Path!\n" (pr-str path)))))