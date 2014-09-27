(ns gbu.web
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]))

(def ^:private authorize-url "https://github.com/login/oauth/authorize")
(def ^:private access-token-url "https://github.com/login/oauth/access_token")

(def ^:private scope "user:email")
(def ^:private client-id "7b43cab01bf06f40d2a0")
(def ^:private client-secret "9f902d8755cbe38672fc89fecd1c8a3ceb539daa")

(html/deftemplate base "templates/base.html"
  [title body]
  [:head :title :span] (html/content (str " - " title)))

(defn home []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (#'base "Home" "Hi")})

(defn login []
  (let [url (str authorize-url
              "?client_id=" client-id
              "&scope=" scope)]
  {:status 302
   :headers {"location" url}}))

(defn callback [code]
  (let [params {:form-params {:client_id client-id
                              :client_secret client-secret
                              :code code}}
        {:keys [body]} (client/post access-token-url params)]
    {:status 200
     :body body}))
