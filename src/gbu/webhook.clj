(ns gbu.webhook
  (:require [clojure.data.json :as json]
            popen)
  (:import java.io.File))

(defn- keywordize 
  [s]
  (-> s .toLowerCase (.replace "_" "-") keyword))

(defn make-tmp-dir []
  (let [dirname (str "gbu-" (java.util.UUID/randomUUID))
        dir     (File. dirname)]
    (.mkdir dir)
    dirname))

(defn clone-repo
  [url sha]
  (let [path       (make-tmp-dir)
        clone      (popen/popen ["git" "clone" url path] :redirect true :dir ".")
        _          (popen/join clone)
        checkout   (popen/popen ["git" "checkout" sha] :redirect true :dir path)
        _          (popen/join checkout)]
    path))

(defn- extract-warnings-and-errors
  [s]
  (-> s
    (.replaceAll "^==.*\n" "")
    (.replaceAll "==.*==\n" "")
    (.replaceAll "(?s)==.*$" "")))

(defn- run-eastwood 
  [dir]
  (let [lein      (popen/popen ["lein" "eastwood"] :redirect true :dir dir)
        code      (popen/join lein)]
    (when-not (zero? code)
      (->> (popen/stdout lein)
        slurp
        extract-warnings-and-errors))))

(defn- handle-pull-req 
  [pr-event]
  (let [pull-req  (:pull_request pr-event)
        sha       (get-in pull-req [:head :sha])
        repo      (:repo pull-req)
        url       (:clone_url repo)
        path      (clone-repo url sha)
        result    (run-eastwood path)]
    ()))

(defmulti handle-event ^:private
  (fn [event-type _] (keywordize event-type)))

(defn run
  [event-type event-raw]
  (let [event (and (seq event-raw)
                   (json/read-str event-raw))]
    (handle-event event-type event)))

(defmethod handle-event :ping
  [_ event]
  {:status 200
   :body "pong"})

(defmethod handle-event :pull-request
  [_ event]
  (future (handle-pull-req event))
  {:status 200
   :body "Your PR is being processed..."})