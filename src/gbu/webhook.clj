(ns gbu.webhook
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [tentacles.pulls :as pulls]
            [gbu.utils :as utils]
            popen)
  (:import java.io.File))

(def github-basic-auth (or (System/getenv "GITHUB_BASIC_AUTH") "goodbadugly:ClojureCup2014"))

(defn clone-repo
  [url sha]
  (let [path       (utils/make-tmp-dir)
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

(defn- result-full-path
  "Does a best effort to match the path returned
by eastwood to a file in the project."
  [files project-dir {path :file :as result}]
  (if-let [file (utils/find-file files path)]
    (assoc result :file (utils/relative-to-dir project-dir file))
    result))

(defn- match-pr-file
  "Checks if the result matches any files that 
are included in the PR files and adds the pr-file
to the result under the key :pull-req-file."
  [pr-files {:keys [file] :as result}]
  (if-let [pr-file (->> pr-files
                     (filter #(= file (:filename %)))
                     first)]
    (assoc result :pull-req-file pr-file)
    result))

(defn- comment-exists? 
  "Returns true if the comment exists in the same 
file, the same line and the content is the same."
  [{:keys [file msg] :as result} rel-line comments]
  (some (fn [{:keys [position body path]}]
          (and
            (= file path)
            (= rel-line position)
            (= msg body)))
    comments))

(defn- create-comment
  "Creates a comment only if the same comment doesn't 
exist already."
  [user reponame id sha comments 
   {:keys [pull-req-file line msg file] :as result}]
  (let [patch    (:patch pull-req-file)
        rel-line (utils/abs-line->patch-line patch line)]
    (when-not (comment-exists? result rel-line comments)
      (pulls/create-comment 
        user reponame id sha
        file rel-line msg 
        {:auth github-basic-auth}))))

(defn- handle-pull-req 
  "Clones the repo and checks out the PR's latest sha.
Does a best effort to match the path returned by eastwood to
a file in the project."
  [pr-event]
  (let [pull-req  (:pull-request pr-event)
        sha       (get-in pull-req [:head :sha])
        repo      (get-in pull-req [:head :repo])
        url       (:clone-url repo)
        pr-id     (:number pr-event)
        user      (get-in repo [:owner :login])
        reponame  (:name repo)
        pr-files  (pulls/files user reponame pr-id {:auth github-basic-auth})]
    (println "Checking if any PR file is a Clojure file...")
    (when (->> pr-files
            (map :filename)
            (filter utils/clojure-file?))
      (let [dirname   (clone-repo url sha)
            results   (run-eastwood dirname)]
        (when results
          (println "Eastwood made" (count results) "comment.")
          (let [files    (file-seq (io/file dirname))
                comments (pulls/comments user reponame pr-id {:auth github-basic-auth})
                cnt      (->> results 
                           (map (partial result-full-path files dirname))
                           (map (partial match-pr-file pr-files))
                           (filter :pull-req-file)
                           (map (partial create-comment user reponame pr-id sha comments))
                           (filter identity)
                           count)]
            (println "Deleting temp directory" dirname)
            (utils/delete-recursively dirname)
            (println "Created" cnt "comments.")))))))

(defmulti handle-event ^:private
  (fn [event-type _] (utils/keywordize event-type)))

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
  (if-let [event (and (seq event-raw)
                   (json/read-str event-raw :key-fn utils/keywordize))]
    (handle-event event-type event)
    {:status 400
     :body "No PR data."}))
