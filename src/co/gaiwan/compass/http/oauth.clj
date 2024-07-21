(ns co.gaiwan.compass.http.oauth
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.util :as util]
   [datomic.api :as d]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]
   [ring.util.response :as response]))

(def discord-oauth-endpoint "https://discord.com/oauth2/authorize")
(def discord-api-endpoint "https://discord.com/api/v10")

(def default-scopes  ["email" "identify"])

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
        state                 (get query-params "state")
        {:keys [status body]} (exchange-code code)
        redirect-url (:oauth/redirect-url (db/entity [:oauth/state-id (java.util.UUID/fromString state)]))]
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
              :discord/expires-at    (.plusSeconds (java.time.Instant/now) (- expires_in 60))}]]
        (def tx-data tx-data)
        @(db/transact tx-data )
        {:status  302
         :headers {"Location" (or redirect-url "/")}
         :flash   [:p "You are signed in!"]
         :session {:identity user-uuid}}))))

(defn routes []
  [""
   ["/oauth2"
    ["/discord"
     ["/callback"
      {:get {:handler GET-callback}}]]]
   ["/logout"
    {:get {:handler (fn [req]
                      (assoc
                       (response/redirect "/")
                       :flash "You were signed out"
                       :session {}))}}]])
