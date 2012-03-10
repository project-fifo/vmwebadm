(ns server.http
  (:use [server.utils :only [clj->js prn-js clj->json]]))

(defn write [response code headers content]
  (.writeHead response code (clj->js headers))
  (.end response content))


(defn error [response error]
  (http/write response 500
              {"Content-Type" "application/json"}
              (clj->json {:error error})))

(defn respond [content-type formater res content]
  (write res 200 {"Content-Type" content-type}
         (formater (or content ""))))


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
  (get ext-map ext res-json))

(defn with-reqest-body [request response callback]
  (let [body (atom "")]
    (.on request "data"
         (fn [data]
           (swap! body str data)))
    (.on request "end"
         (fn []
           (let [r (if (= @body "")
                     {}
                     (js->clj (.parse js/JSON @body)))]
             (callback r ))))))