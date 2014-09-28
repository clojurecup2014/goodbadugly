(ns gbu.webhook)

(defn run
  [event-type event-raw]
  (prn event-type event-raw)
  {:status 200})
