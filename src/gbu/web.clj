(ns gbu.web
  (:require [net.cgrand.enlive-html :as html]))

(html/deftemplate base "templates/base.html"
  [title body]
  [:head :title :span] (html/content (str " - " title)))


