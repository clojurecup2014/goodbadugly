(ns gbu.web
  (:require [net.cgrand.enlive-html :as html]
            [gbu.api :as api]))

(html/deftemplate base "templates/base.html"
  [title content]
  [:head :title :span] (html/content (str " - " title))
  [:div#content] (html/content content))

(html/defsnippet home-snip "templates/snippets.html" [:#home]
  [])

(html/defsnippet repos-snip "templates/snippets.html" [:#repos]
  [])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Web handlers

(defn home
  []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (#'base "Home" (home-snip))})

(defn repos
  [cookies]
  (if-let [token (api/token cookies)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (#'base "Repos" (repos-snip))}
    {:status 302
     :headers {"location" "/"}}))
