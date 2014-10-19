(ns gbu.api
  (:require [clj-http.client :as client]
            [tentacles.repos :as repos]
            [tentacles.orgs :as orgs]
            [clojure.data.json :as json]
            [gbu.utils :as utils]))

(def ^:private authorize-url "https://github.com/login/oauth/authorize")
(def ^:private access-token-url "https://github.com/login/oauth/access_token")

(def ^:private scope "repo,user:email")
(def ^:private client-id (utils/env "GBU_CLIENT_ID"))
(def ^:private client-secret (utils/env "GBU_CLIENT_SECRET"))

(def ^:private webhook-config
  {:url "http://goodbadugly.clojurecup.com/api/webhook"
   :content_type "json"})

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
               "set-cookie" (str "token=" token ";Path=/")}}))

(defn- owner?
  [repo]
  (get-in repo [:permissions :admin]))

(defn- get-webhook
  [token user reponame]
  (let [hooks        (repos/hooks user reponame {:oauth-token token})
        gbu-webhook? #(= (:url webhook-config)
                         (get-in % [:config :url]))]
    (->> hooks
      (filter gbu-webhook?)
      first)))


(defn- add-status
  [token repo]
  (let [user     (get-in repo [:owner :login])
        reponame (:name repo)
        status   (if (get-webhook token user reponame) :on :off)]
    (assoc repo :gbu status)))

(defn- all-user-repos
  [token]
  (let [opts       {:oauth-token token
                    :type :owner}
        user-repos (repos/repos opts)
        orgs-repos (->> (orgs/orgs opts)
                     (mapcat #(repos/org-repos (:login %) opts))
                     (filter owner?))]
    (->> (into user-repos orgs-repos)
      (map (partial add-status token)))))

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
  [token repo]
  (let [owner (:owner repo)]
    (if (= (:type owner) "Organization")
      (do
        (prn "Create Services team")
        (prn "Add user to team"))
      (prn "Add user as collaborator"))))

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
  [cookies user reponame]
  (let [token   (token cookies)
        webhook (get-webhook token user reponame)]
    (when webhook
      (repos/delete-hook user reponame (:id webhook) {:oauth-token token}))
    {:status 200
     :body "done"}))
