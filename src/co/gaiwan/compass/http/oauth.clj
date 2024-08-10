(ns co.gaiwan.compass.http.oauth
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.util :as util]
   [datomic.api :as d]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]
   [ring.util.response :as response]
   [io.pedestal.log :as log])
  (:import (java.time Instant)))

(def discord-oauth-endpoint "https://discord.com/oauth2/authorize")
(def discord-api-endpoint "https://discord.com/api/v10")


(def default-scopes  ["email" "identify" "guilds.join" "role_connections.write"])

(defn bot-auth-headers []
  {"Authorization" (str "Bot " (config/value :discord/bot-token))})


(defn flow-init-url
  ([]
   (flow-init-url nil))
  ([{:keys [scopes redirect-url]
     :or {scopes default-scopes}}]
   (let [state (random-uuid)]
     (when redirect-url
       @(db/transact [{:oauth/state-id     state
                       :oauth/redirect-url redirect-url}]))
     (-> (uri/uri discord-oauth-endpoint)
         (uri/assoc-query*
          {:client_id     (config/value :discord/client-id)
           :response_type "code"
           :redirect_uri  (str (config/value :compass/origin) "/oauth2/discord/callback")
           :scope         (str/join " " scopes)
           :state         state})))))

;; Add as bot to server
#_
(println (str (flow-init-url ["bot" "applications.commands"])))

(defn request-token [params]
  (hato/post
   (str discord-api-endpoint "/oauth2/token")
   {:as :auto
    :form-params params
    :basic-auth
    {:user (config/value :discord/client-id)
     :pass (config/value :discord/client-secret)}}))

(defn exchange-code [code]
  (request-token
   {:grant_type "authorization_code"
    :code code
    :redirect_uri (str (config/value :compass/origin) "/oauth2/discord/callback")}))

(defn refresh-token [refresh-token]
  (request-token
   {:grant_type "refresh_token"
    :refresh_token refresh-token}))

(defn- expires-in->instant
  [expires-in]
  (.plusSeconds (Instant/now) (- expires-in 60)))

(defn current-access-token
  "Get a valid Discord bearer access token for a user.

  This assumes there already is an access token in the database.
  The function performs a token refresh if the old access token is considered to be expired.
  If refreshing the token fails, returns `nil`.
  There are no guarantees regarding the lifetime of the returned access token, so this function
  should be called for every request to Discord."
  [user-id]
  (let [oauth-data
        (db/q '[:find (pull ?u [:discord/access-token
                                :discord/refresh-token
                                :discord/expires-at])
                :in $ ?uid
                :where [?u :user/uuid ?uid]]
              (db/db)
              user-id)]
    ;; check if token is expired, do a refresh in that case
    (if (.isAfter (Instant/now) (:discord/expires-at oauth-data))
      (let [{:keys [body status]} (refresh-token (:discord/refresh-token oauth-data))]
        (when (= status 200)
          (db/transact [{:user/uuid user-id
                         :discord/access-token (:access_token body)
                         :discord/refresh-token (:refresh_token body)
                         :discord/expires-at (expires-in->instant (:discord/expires_in body))}])
          (:access_token body)))
      (:discord/access-token oauth-data))))


(defn fetch-user-info [token]
  (:body
   (hato/get (str discord-api-endpoint "/users/@me")
             {:as :auto
              :oauth-token token})))

(defn join-server [token]
  (let [{:keys [id username]} (fetch-user-info token)]
    (log/trace :discord/adding-user username)
    (hato/put
     (str discord-api-endpoint "/guilds/" (config/value :discord/server-id) "/members/" id)
     {:as :auto
      :content-type :json
      :form-params {:access_token token}
      :headers (bot-auth-headers)})))

(defn GET-callback [{:keys [query-params]}]
  (let [code                  (get query-params "code")
        {:keys [status body]} (exchange-code code)]
    (if (not= 200 status)
      {:status  302
       :headers {"Location" "/"}
       :flash   [:p
                 "Discord OAuth2 exchange failed."
                 [:pre (util/pprint-str body)]]
       :session {:identity nil}}
      (let [{:keys [access_token refresh_token expires_in]} body
            {:keys [id global_name email username]}         (fetch-user-info access_token)
            user-uuid                                       (:user/uuid (d/entity (db/db) [:user/email email]) (random-uuid))
            tx-data
            [{:user/uuid             user-uuid
              :user/email            email
              :user/name             global_name
              :user/handle           username
              :discord/id            id
              :discord/access-token  access_token
              :discord/refresh-token refresh_token
              :discord/expires-at (expires-in->instant expires_in)}]
            {:keys [status]} (join-server access_token)]
        @(db/transact tx-data)
        {:status  302
         :headers {"Location" "/"}
         :flash   [:p "You are signed in!"
                   (when-not (= status 200)
                     [:br "Unfortunately, adding you to our Discord server didn't work."])]
         :session {:identity user-uuid}}))))

(defn routes []
  [""
   ["/oauth2"
    ["/discord"
     ["/callback"
      {:get {:handler #'GET-callback}}]]]
   ["/logout"
    {:get {:handler (fn [req]
                      (assoc
                       (response/redirect "/")
                       :flash "You were signed out"
                       :session {}))}}]])
