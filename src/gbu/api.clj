(ns gbu.api
  (:require [clj-http.client :as client]
            [tentacles.repos :as repos]
            [tentacles.orgs :as orgs]
            [clojure.data.json :as json]))

(def ^:private authorize-url "https://github.com/login/oauth/authorize")
(def ^:private access-token-url "https://github.com/login/oauth/access_token")

(def ^:private scope "repo,user:email")
(def ^:private client-id "7b43cab01bf06f40d2a0")
(def ^:private client-secret "9f902d8755cbe38672fc89fecd1c8a3ceb539daa")

(def ^:private webhook-config
  {:url "http://goodbadugly.clojurecup.com/api/webhook"
   :content_type "application/json"})

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

(defn- owner?
  [repo]
  (get-in repo [:permissions :admin]))

(defn- all-user-repos
  [token]
  (let [opts       {:oauth-token token
                    :type :owner}
        user-repos (repos/repos opts)
        orgs-repos (->> (orgs/orgs opts)
                     (mapcat #(repos/org-repos (:login %) opts))
                     (filter owner?))]
    (into user-repos orgs-repos)))

(defn repos 
  [cookies]
  (if-let [token (token cookies)]
    {:status 200
     :body (json/write-str (all-user-repos token))}
    {:status 403
     :body "Token missing."}))

(defn- create-webhook
  [token user repo]
  (let [opts    {:oauth-token token 
                 :active true
                 :events "pull_request"}]
    (repos/create-hook user repo "web" webhook-config opts)))

(defn- add-gbu-user
  [token repo])

(defn on
  [cookies user reponame]
  (let [token   (token cookies)
        repo    (repos/specific-repo user reponame {:oauth-token token})]
    (create-webhook token user reponame)
    (when (:private repo)
      (add-gbu-user token repo))
    {:status 200
     :body "done"}))

(defn off
  [cookies]
  {:status 200
   :body "done"})
