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
(def ^:private url (utils/env "GBU_URL" "https://goodbadugly-clj.herokuapp.com"))

(def ^:private webhook-config
  {:url (str url "/api/webhook")
   :content_type "json"})

(defn- qs-map
  "Given a querystring builds a map with its keys and values."
  [qs]
  (let [split (fn [x s] (map str (.split x s)))]
    (->> (split qs "&")
      (map #(split % "="))
      (map vec)
      (into {}))))

(defn token
  "Given the map of cookies returns the value of
the GitHub's token if present or nil otherwise."
  [cookies]
  (when-let [{:keys [value]} (cookies "token")]
    value))

(defn login
  "Starts the OAuth process."
  []
  (let [url (str authorize-url
              "?client_id=" client-id
              "&scope=" scope)]
  {:status 302
   :headers {"location" url}}))

(defn callback
  "Handles the callback from GitHub OAuth flow."
  [code]
  (let [params {:form-params {:client_id client-id
                              :client_secret client-secret
                              :code code}}
        {:keys [body]} (client/post access-token-url params)
        token  ((qs-map body) "access_token")]
    {:status 302
     :headers {"location" "/repos"
               "set-cookie" (str "token=" token ";Path=/")}}))

(defn- owner?
  "Returns true if the user has admin permissions over the 
repo and false otherwise."
  [repo]
  (get-in repo [:permissions :admin]))

(defn- get-webhook
  "Given a repository checks if it has a specific webhook
enables. Returns the webhook if present or nil otherwise."
  [token user reponame]
  (let [hooks        (repos/hooks user reponame {:oauth-token token})
        gbu-webhook? #(= (:url webhook-config)
                         (get-in % [:config :url]))]
    (->> hooks
      (filter gbu-webhook?)
      first)))


(defn- add-status
  "Given a repo adds the status of the webhook as either
:on of :off in the :gbu key."
  [token repo]
  (let [user     (get-in repo [:owner :login])
        reponame (:name repo)
        status   (if (get-webhook token user reponame) :on :off)]
    (assoc repo :gbu status)))

(defn- all-user-repos
  "Returns a list of repositories where the user
has the necessary permissions to create a webhook."
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
  "Returns all of the repos in which the user has
permission for creating webhooks."
  [cookies]
  (if-let [token (token cookies)]
    {:status 200
     :body (json/write-str (all-user-repos token))}
    {:status 403
     :body "Token missing."}))

(defn- create-webhook
  "Create a webhook in the specified repo."
  [token user repo]
  (let [opts    {:oauth-token token
                 :active true
                 :events "pull_request"}]
    (repos/create-hook user repo "web" webhook-config opts)))

(defn- add-gbu-user
  "Add the GBU user to the repository. 
TODO: for private repositories the user should be
either added as a collaborator (user repo) or as 
part of a team (org repo)."
  [token repo]
  (let [owner (:owner repo)]
    (if (= (:type owner) "Organization")
      (do
        (prn "Create Services team")
        (prn "Add user to team"))
      (prn "Add user as collaborator"))))

(defn on
  "Enables the webhook for the specified repository."
  [cookies user reponame]
  (let [token   (token cookies)
        repo    (repos/specific-repo user reponame {:oauth-token token})]
    (create-webhook token user reponame)
    (when (:private repo)
      (add-gbu-user token repo))
    {:status 200
     :body "done"}))

(defn off
  "Disables the webhook for the specified repository."
  [cookies user reponame]
  (let [token   (token cookies)
        webhook (get-webhook token user reponame)]
    (when webhook
      (repos/delete-hook user reponame (:id webhook) {:oauth-token token}))
    {:status 200
     :body "done"}))
