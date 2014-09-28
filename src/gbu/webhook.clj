(ns gbu.webhook
  (:require [clojure.data.json :as json]))

(defn run
  [event-type event-raw]
  (prn event-type (json/read-str event-raw))
  {:status 200})
