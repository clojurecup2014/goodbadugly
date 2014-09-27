(ns gbu.core
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [gbu.web :as web]))

(defn http-port 
  []
  (or (System/getenv "GBU_HTTP_PORT") 5000))

(defroutes app
  (GET "/" [] (web/home))
  (GET "/login" [] (web/login))
  (GET "/callback" [code] (web/callback code))
  (route/resources "/")
  (route/not-found "Oops 404"))

(defn -main [& [port]]
  (jetty/run-jetty (site #'app) {:port (http-port) :join? false}))
