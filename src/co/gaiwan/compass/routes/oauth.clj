(ns co.gaiwan.compass.routes.oauth
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.auth :as auth-html]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.util :as util]
   [datomic.api :as d]
   [lambdaisland.uri :as uri]))

(defn GET-discord-redirect
  "Kick off the Discord OAuth flow, by saving the necessary OAuth state on our
  side, and then redirecting to Discord."
  [{:keys [query-params session]}]
  (let [state (random-uuid)]
    @(db/transact [{:oauth/state-id     state
                    :oauth/redirect-url (get query-params "redirect_url" "/")}])
    (-> (uri/uri oauth/discord-oauth-endpoint)
        (uri/assoc-query*
         {:client_id     (config/value :discord/client-id)
          :response_type "code"
          :redirect_uri  (str (config/value :compass/origin) "/oauth2/discord/callback")
          :scope         (str/join " " oauth/default-scopes)
          :state         state})
        str
        response/redirect
        (assoc :session (assoc session :oauth/state-id state)))))

(defn GET-discord-callback [{:keys [query-params session]}]
  (doto
      (let [{:strs [code state]}  query-params
            _
            (prn code state)
            {:keys [status body]} (oauth/exchange-code code)]
        (prn status body)
        (prn session)
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
          (let [{:keys [access_token refresh_token expires_in]} body
                {:keys [id global_name email username]}         (discord/fetch-user-info access_token)
                user-uuid                                       (:user/uuid (d/entity (db/db) [:user/email email]) (random-uuid))
                tx-data
                [{:user/uuid             user-uuid
                  :user/email            email
                  :user/name             global_name
                  :user/handle           username
                  :discord/id            id
                  :discord/access-token  access_token
                  :discord/refresh-token refresh_token
                  :discord/expires-at    (util/expires-in->instant expires_in)}]
                {:keys [status]}                                (discord/join-server access_token)]
            @(db/transact tx-data)
            {:status  302
             :headers {"Location" (:oauth/redirect-url (db/entity [:oauth/state-id (parse-uuid state)]) "/")}
             :flash   [:p "You are signed in!"
                       (case status
                         204 nil
                         201 [:br "You've also been added to "
                              [:a {:href (str "https://discord.com/channels/" (config/value :discord/server-id))}
                               "our Discord server"] "!"]
                         [:br "Unfortunately, adding you to our Discord server didn't work."])]
             :session {:identity user-uuid}})))
    prn))

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
                        :flash "You were signed out"
                        :session {}))}}]])
