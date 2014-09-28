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

(defn- patch-position
  [line]
  (let [re     #"^@@ .*? \+(\d+),.*$"
        [_ number] (re-matches re line)]
    (Integer/parseInt number)))

(comment
 (patch-position "@@ -109,7 +109,7 @@ option_spec_list() ->")
 (def patch (str "@@ -109,7 +109,7 @@ option_spec_list() ->\n     [\n      {help,"
              " $h, \"help\", undefined, \"Show this help information.\"},\n"
              "      {config, $c, \"config\", string, Commands},\n-     "
              "{commands, undefined, \"commands\", undefined, \"Show available"
              " commands.\"}\n+     {commands, undefined, \"commands\", "
              "undefined, \"Show available commands.\"} %% Long Line\n    "
              " ].\n \n -spec process_options([atom()], [string()]) -> ok."
              "\n@@ -175,3 +175,5 @@ git-hook         Pre-commit Git Hook: "
              "Gets all staged files and runs the rules\n                  "
              "                     files.\n \">>,\n    io:put_chars(Commands)."
              "\n+\n+%% Another dummy change to check how patches are built "
              "with changes wide apart."))
  (abs-line->patch-line patch 112)
 )
