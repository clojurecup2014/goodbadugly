(ns gbu.api
  (:require [clj-http.client :as client]
            [tentacles.repos :as repos]
            [clojure.data.json :as json]))

(def ^:private authorize-url "https://github.com/login/oauth/authorize")
(def ^:private access-token-url "https://github.com/login/oauth/access_token")

(def ^:private scope "repo,user:email")
(def ^:private client-id "7b43cab01bf06f40d2a0")
(def ^:private client-secret "9f902d8755cbe38672fc89fecd1c8a3ceb539daa")

(defn- qs-map 
  [qs]
  (let [split (fn [x s] (map str (.split x s)))]
    (->> (split qs "&")
      (map #(split % "="))
      (map vec)
      (into {}))))

(defn token [cookies]
  (when-let [{:keys [value]} (cookies "token")]
    value))

(defn login
  []
  (let [url (str authorize-url
              "?client_id=" client-id
              "&scope=" scope)]
  {:status 302
   :headers {"location" url}}))

(defn callback
  [code]
  (let [params {:form-params {:client_id client-id
                              :client_secret client-secret
                              :code code}}
        {:keys [body]} (client/post access-token-url params)
        token  ((qs-map body)"access_token")]
    {:status 302
     :headers {"location" "/repos"
               "set-cookie" (str "token=" token)}}))

(defn repos 
  [cookies]
  (let [opts      {:oauth-token (token cookies)
                   :type :owner}
        user-repos (repos/repos opts)]
    (prn "repos: " (count user-repos))
    {:status 200
     :body (json/write-str user-repos)}))

