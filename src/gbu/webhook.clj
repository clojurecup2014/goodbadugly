(ns gbu.webhook
  (:require [clojure.data.json :as json]))

(defn run
  [event-type event-raw]
  (let [event (and (seq event-raw)
                   (json/read-str event-raw))]
    (prn event-type event)
    {:status 200
     :body "done"}))
