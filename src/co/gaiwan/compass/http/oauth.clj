(ns co.gaiwan.compass.http.oauth
  (:require
   [co.gaiwan.compass.config :as config]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]
   [datomic.api :as d]
   [co.gaiwan.compass.db :as db]))

(def discord-oauth-endpoint "https://discord.com/oauth2/authorize")
(def discord-api-endpoint "https://discord.com/api/v10")

(defn flow-init-url []
  (-> (uri/uri discord-oauth-endpoint)
      (uri/assoc-query*
       {:client_id     (config/value :discord/app-id)
        :response_type "code"
        :redirect_uri  (str (config/value :compass/origin) "/oauth2/discord/callback")
        :scope         "identify email"})))

(defn exchange-code [code]
  (hato/post
   (str discord-api-endpoint "/oauth2/token")
   {:as :auto
    :form-params
    {:grant_type "authorization_code"
     :code code
     :redirect_uri (str (config/value :compass/origin) "/oauth2/discord/callback")}
    :basic-auth
    {:user (config/value :discord/client-id)
     :pass (config/value :discord/client-secret)}}))

(defn fetch-user-info [token]
  (:body
   (hato/get (str discord-api-endpoint "/users/@me")
             {:as :auto
              :headers {"Authorization" (str "Bearer " token)}})))

(defn GET-callback [{:keys [query-params]}]
  (let [code                  (get query-params "code")
        {:keys [status body]} (exchange-code code)]
    (when (= 200 status)
      (let [{:keys [access_token refresh_token expires_in]} body
            {:keys [global_name email username]}            (fetch-user-info access_token)]
        (d/transact
         (db/conn)
         [{:user/email            email
           :user/name             global_name
           :user/handle           username
           :discord/access-token  access_token
           :discord/refresh-token refresh_token
           :discord/expires-at    (.plusSeconds (java.time.Instant/now) (- expires_in 60))}])))
    {:status  302
     :headers {"Location" "/"}
     :flash   (if (= 200 status)
                "You are signed in"
                "Discord OAuth2 exchange failed. Try again?")
     #_:session
     }))

(defn routes []
  ["/oauth2"
   ["/discord"
    ["/callback"
     {:get {:handler GET-callback}}]]])
