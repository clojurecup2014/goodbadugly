(ns gbu.core
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [gbu.web :as web]
            [gbu.api :as api]
            [gbu.webhook :as webhook]))

(defn http-port
  []
  (let [env-port (System/getenv "GBU_HTTP_PORT")]
    (or (and env-port (Integer/parseInt env-port)) 5000)))

(defroutes app
  ;; Pages
  (GET "/" [] (web/home))
  (GET "/repos" {cookies :cookies} (web/repos cookies))
  (GET "/config" [] (web/config))
  (GET "/contact" [] (web/contact))
  (GET "/vote" [] (web/vote))
  ;; Endpoints
  (GET "/api/login"
    []
    (api/login))
  (GET "/api/callback"
    [code]
    (api/callback code))
  (GET "/api/repos"
    {cookies :cookies} 
    (api/repos cookies))
  (GET "/api/on"
    {cookies :cookies {repo :repo user :user} :params}
    (api/on cookies user repo))
  (GET "/api/off"
    {cookies :cookies {repo :repo user :user} :params}
    (api/off cookies user repo))
  (POST "/api/webhook"
    {headers :headers body :body}
    (let [event-raw  (slurp body)
          event-type (headers "x-github-event")]
      (webhook/run event-type event-raw)))
  (route/resources "/")
  (route/not-found "Oops 404"))

(defn -main [& [port]]
  (jetty/run-jetty (site #'app) {:port (http-port) :join? false}))
