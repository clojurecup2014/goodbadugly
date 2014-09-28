(ns gbu.webhook
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
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
        _          (println "Cloning" url "to" path "...")
        _          (popen/join clone)
        checkout   (popen/popen ["git" "checkout" sha] :redirect true :dir path)
        _          (println "Checking out" sha "...")
        _          (popen/join checkout)]
    path))

(defn- run-eastwood 
  [path]
  (let [lein      (popen/popen ["lein" "eastwood" "{:results-file true}"] :redirect true :dir path)
        _          (println "Running eastwood...")
        code      (popen/join lein)]
    (when-not (zero? code)
      (let [results-txt (slurp (str path "/eastwood-results.txt"))]
        (read-string (str "[" results-txt "]"))))))

(defn- find-file [files ends-with]
  (->> files
    (filter #(.endsWith (.getCanonicalPath %) ends-with))
    first))

(defn- relative-to-project
  [dir file]
  (-> file
    .getCanonicalPath
    (.replaceAll (str ".*/" dir "/") "")))

(defn- result-full-path
  [files project-dir {path :file :as result}]
  (if-let [file (find-file files path)]
    (assoc result :file (relative-to-project project-dir file))
    result))

(defn- handle-pull-req 
  "Clones the repo and checks out the PR's latest sha.
Does a best effort to match the path returned by eastwood to
a file in the project."
  [pr-event]
  (let [pull-req  (:pull_request pr-event)
        sha       (get-in pull-req [:head :sha])
        repo      (get-in pull-req [:head :repo])
        url       (:clone_url repo)
        dirname   (clone-repo url sha)
        results   (run-eastwood dirname)]
    (when results
      (let [files   (file-seq (io/file dirname))
            results (map (partial result-full-path files dirname) results)]
        
        results))))

(defmulti handle-event ^:private
  (fn [event-type _] (keywordize event-type)))

(defmethod handle-event :ping
  [_ event]
  {:status 200
   :body "pong"})

(defmethod handle-event :pull-request
  [_ event]
  (future (handle-pull-req event))
  {:status 200
   :body "Your PR is being processed..."})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API

(defn run
  [event-type event-raw]
  (let [event (and (seq event-raw)
                   (json/read-str event-raw :key-fn keyword))]
    (handle-event event-type event)))
