(ns co.gaiwan.compass.http.oauth
  "Namespace for generic OAuth2 handling (authorization code flow).

  For now, this contains Discord-specific scopes and URLs, like the routes/oauth namespace as well.
  This can eventually be changed."
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.util :as util]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]
   [io.pedestal.log :as log])
  (:import (java.time Instant)))

(def discord-oauth-endpoint "https://discord.com/oauth2/authorize")

(def default-scopes  ["email" "identify" "guilds.join"])

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
   "https://discord.com/api/oauth2/token"
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
                         :discord/expires-at (util/expires-in->instant (:discord/expires_in body))}])
          (:access_token body)))
      (:discord/access-token oauth-data))))
