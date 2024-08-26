(ns co.gaiwan.compass.routes.oauth
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.auth :as auth-html]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.model.user :as user]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.util :as util]
   [datomic.api :as d]
   [io.pedestal.log :as log]
   [lambdaisland.uri :as uri]))

(defn GET-discord-redirect
  "Kick off the Discord OAuth flow, by saving the necessary OAuth state on our
  side, and then redirecting to Discord."
  [{:keys [query-params]}]
  (let [state (random-uuid)]
    (-> (uri/uri oauth/discord-oauth-endpoint)
        (uri/assoc-query*
         {:client_id     (config/value :discord/client-id)
          :response_type "code"
          :redirect_uri  (str (config/value :compass/origin) "/oauth2/discord/callback")
          :scope         (str/join " " oauth/default-scopes)
          :state         state})
        str
        response/redirect
        (update :session assoc :oauth/state-id state :oauth/redirect-url (get query-params "redirect_url" "/")))))

(defn user-tx [user-uuid
               {:keys [access_token refresh_token expires_in] :as body}
               {:keys [id email global_name] :as user-info}]
  #_(def user-info user-info)
  (let [discord-avatar-url (str "https://cdn.discordapp.com/avatars/" id "/" (:avatar user-info) ".png")
        avatar-url (try
                     (user/download-avatar discord-avatar-url)
                     (catch Exception e
                       (log/warn :discord/avatar-download-failed {:url discord-avatar-url}
                                 :exception e)
                       discord-avatar-url))]
    [{:user/uuid                 user-uuid
      :public-profile/name       global_name
      :public-profile/avatar-url avatar-url
      :discord/id                id
      :discord/email             email
      :discord/access-token      access_token
      :discord/refresh-token     refresh_token
      :discord/expires-at        (util/expires-in->instant expires_in)}]))

(defn GET-discord-callback [{:keys [query-params session]}]
  (let [{:strs [code state]}  query-params
        {:keys [status body]} (oauth/exchange-code code)]
    (cond
      (not= 200 status)
      (-> (response/redirect "/" {:flash [:p
                                          "Discord OAuth2 exchange failed."
                                          [:pre (util/pprint-str body)]]})
          (assoc :session {}))

      (not= state (str (:oauth/state-id session)))
      (-> (response/redirect "/" {:flash [:p "Discord OAuth2 invalid state."]})
          (assoc :session {}))

      :else
      (let [{:keys [id] :as user-info} (discord/fetch-user-info (:access_token body))
            user-uuid                  (:user/uuid (d/entity (db/db) [:discord/id id]) (random-uuid))
            {:keys [status]}           (discord/join-server (:access_token body))]
        @(db/transact (user-tx user-uuid body user-info))
        {:status  302
         :headers {"Location" (:oauth/redirect-url session "/")}
         :flash   [:p "Welcome to Compass, " (:global_name user-info) "!"
                   (case status
                     204 nil
                     201 [:br "You've also been added to "
                          [:a {:href (str "https://discord.com/channels/" (config/value :discord/server-id))}
                           "our Discord server"] "!"]
                     [:br "Unfortunately, adding you to our Discord server didn't work."])]
         :session {:identity user-uuid}}))))

(defn GET-login [req]
  {:html/layout false
   :html/body [auth-html/popup (-> req :params :next)]
   :headers {"HX-Retarget" "#modal"
             "HX-Reselect" (str "." auth-html/popup)}})

(defn routes []
  [""
   ["/oauth2"
    ["/discord"
     ["/redirect"
      {:get {:handler GET-discord-redirect}}]
     ["/callback"
      {:get {:handler GET-discord-callback}}]]]
   ["/login" {:get {:handler GET-login}}]
   ["/logout"
    {:get {:handler (fn [req]
                      (assoc
                        (response/redirect "/")
                        :flash "Thank you for using Compass! Please come again."
                        :session {}))}}]])
