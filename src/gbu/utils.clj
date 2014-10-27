(ns gbu.utils
  (:import java.io.File))

(defn keywordize
  [s]
  (-> s .toLowerCase (.replace "_" "-") keyword))

(defn make-tmp-dir []
  (let [dirname (str "gbu-" (java.util.UUID/randomUUID))
        dir     (File. dirname)]
    (.mkdir dir)
    dirname))

(defn find-file
  [files ends-with]
  (->> files
    (filter #(.endsWith (.getCanonicalPath %) ends-with))
    first))

(defn relative-to-dir
  [dir file]
  (-> file
    .getCanonicalPath
    (.replaceAll (str ".*/" dir "/") "")))

(defn clojure-file?
  [filename]
  (.contains (.replaceAll filename ".*\\." "") "clj"))


(defn- patch-position
  [line]
  (let [re     #"^@@ .*? \+(\d+),.*$"
        [_ number] (re-matches re line)]
    (Integer/parseInt number)))

(defn- patch-line-type [line]
  (case (first line)
    \@ :patch
    \+ :add
    \- :del
    \  :same))

(defn- new-position
  [line [local global]]
  (case (patch-line-type line)
    :patch  [(inc local) (dec (patch-position line))]
    :del    [(inc local) global]
    :add    [(inc local) (inc global)]
    :same   [(inc local) (inc global)]))

(defn abs-line->patch-line
  "Takes the string for a patch and returns
the line relative to it or nil if it's outside
the patch."
  [patch line-num]
  (loop [[line & lines] (.split patch "\n")
         positions      [-1, nil]]
    (when line
      (let [line-type (patch-line-type line)
            [local global :as new-pos] (new-position line positions)]
        (if (and (= global line-num)
                 (not (#{:patch :del} line-type)))
          local
          (recur lines new-pos))))))

(defn delete-recursively
  [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (clojure.java.io/delete-file f))]
    (func func (clojure.java.io/file fname))))

(defn env [var-name & [default]]
  (or (System/getenv var-name) default))
