(defproject gbu "0.1.0-SNAPSHOT"
  :description "Check your Clojure code with Eastwood."
  :url "http://goodbadugly.clojurecup.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [enlive "1.1.5"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [clj-http "1.0.0"]
                 [tentacles "0.2.7"]
                 [org.clojure/data.json "0.2.5"]]
  :main gbu.core
  :uberjar-name "gbu.jar")
