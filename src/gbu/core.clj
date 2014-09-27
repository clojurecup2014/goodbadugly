(ns gbu.core
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]))

(defn home []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello"})

(defroutes app
  (GET "/" []
    (home))
  (ANY "*" []
    (route/not-found "Oops 404")))

(defn -main [& [port]]
  (let [port (int 5000)]
    (jetty/run-jetty (site #'app) {:port port :join? false})))
