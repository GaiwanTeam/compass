(ns co.gaiwan.compass.http.oauth
  "Namespace for generic OAuth2 handling (authorization code flow).

  For now, this contains Discord-specific scopes and URLs, like the routes/oauth namespace as well.
  This can eventually be changed."
  (:require
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.util :as util]
   [hato.client :as hato]
   [java-time.api :as time]))

(def discord-oauth-endpoint "https://discord.com/oauth2/authorize")

(def default-scopes  ["email" "identify" "guilds.join"])

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

  Takes a user info map with keys `:discord/access-token`, `:discord/refresh-token`, `:discord/expires-at`, and `:user/uuid`.
  The function performs a token refresh if the old access token is considered to be expired.
  If refreshing the token fails, returns `nil`.
  There are no guarantees regarding the lifetime of the returned access token, so this function
  should be called for every request to Discord."
  [user]
  ;; check if token is expired or will expire soon, do a refresh in that case
  (if (time/> (time/plus (time/zoned-date-time) (time/seconds 10)) (:discord/expires-at user))
    (let [{:keys [body status]} (refresh-token (:discord/refresh-token user))]
      (when (= status 200)
        @(db/transact [{:user/uuid (:user/uuid user)
                        :discord/access-token (:access_token body)
                        :discord/refresh-token (:refresh_token body)
                        :discord/expires-at (util/expires-in->instant (:discord/expires_in body))}])
        (:access_token body)))
    (:discord/access-token user)))
