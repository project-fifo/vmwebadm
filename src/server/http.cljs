(ns server.http
  (:require [clojure.string :as c.s]
            [server.storage :as storage]
            [cljs.nodejs :as node])
  (:use [server.utils :only [clj->js prn-js clj->json nestify-map base64-decode hash-str]]))

(def http-signature
  (node/require "http-signature"))

(defn write [response code headers content]
  (.writeHead response code (clj->js (assoc headers
                                       "X-Api-Version" "6.5.0"
                                       "X-Bark-Version" "0.1.0")))
  (.end response content))

(defn encode-error [msg e]
  {:msg msg
   :error (.-message e)
   :stack (.-stack e)})

(defn ok [response]
  (write response 200
              {"Content-Type" "application/json"}
              "{\"result\": \"ok\"}"))

(defn error
  ([response msg]
     (error 500 "InternalError" msg))
  ([response code e msg]
      (write response code
             {"Content-Type" "application/json"}
             (clj->json {:code code
                         :message e
                         :details msg}))))

(defn e404
  ([response]
     (e404 response ""))
  ([response msg]
     (error response 404 "ResourceNotFound" msg)))

(defn e405
  ([response]
     (e405 response ""))
  ([response msg]
     (error response 405 "MethodNotAllowed" msg)))

(defn e500 [response msg]
  (error response 500 "InternalError" (str msg)))

(defn ret [response data]
  (write response 200
         {"Content-Type" "application/json"}
         (clj->json data)))

(defn not-found [response]
  (error response 404 "ResourceNotFound" "Something was not found."))

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

(defn with-reqest-body [request callback]
  (let [body (atom "")]
    (.on request "data"
         (fn [data]
           (swap! body str data)))
    (.on request "end"
         (fn []
           (let [r (if (= @body "")
                     {}
                     (nestify-map (js->clj (.parse js/JSON @body))))]
             (callback r ))))))

(defn with-passwd-auth [resource response account f]
  (if-let [h (second (c.s/split (get-in resource [:headers "authorization"] "") #" "))]
    (if (= (hash-str (base64-decode h))
           (get-in @storage/data [:users account :passwd]))
      (f)
      (error response 401 "InvalidCredentials" "Bad password or user."))
    (error response 401 "InvalidCredentials" "Auth header missing.")))

(defn with-auth [resource request response account f]
  (try
    (let [parsed  (.parseRequest http-signature request)
          path (next (c.s/split (.-keyId parsed) #"/"))
          pki-account (first path)
          key (last path)
          path [:users account :keys key :web]]
      (if (= account pki-account) 
        (if-let [pub (get-in @storage/data path)]
          (if (.verifySignature http-signature parsed pub)
            (f)
            (error response 401 "InvalidCredentials" "Key verification failed."))
          (error response 401 "InvalidCredentials" "Key not found."))
        (error response 401 "InvalidCredentials" "Wrong pki user.")))
    (catch js/Error e
      (try
        (with-passwd-auth resource response account f)
        (catch js/Error e
          (print "\n==========\n\n"  (.-message e) "\n" (.-stack e) "\n==========\n\n")
          (e500 response (encode-error "Error during not logged in dispatch." e)))))))