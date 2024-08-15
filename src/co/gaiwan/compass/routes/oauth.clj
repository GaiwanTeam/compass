(ns co.gaiwan.compass.routes.oauth
  (:require
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.auth :as auth-html]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.util :as util]
   [co.gaiwan.compass.services.discord :as discord]
   [datomic.api :as d]
   [ring.util.response :as response]))

(defn GET-callback [{:keys [query-params]}]
  (let [code                  (get query-params "code")
        {:keys [status body]} (oauth/exchange-code code)]
    (if (not= 200 status)
      {:status  302
       :headers {"Location" "/"}
       :flash   [:p
                 "Discord OAuth2 exchange failed."
                 [:pre (util/pprint-str body)]]
       :session {:identity nil}}
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
              :discord/expires-at (util/expires-in->instant expires_in)}]
            {:keys [status]} (discord/join-server access_token)]
        @(db/transact tx-data)
        {:status  302
         :headers {"Location" "/"}
         :flash   [:p "You are signed in!"
                   (when-not (= 2 (quot status 100))
                     [:br "Unfortunately, adding you to our Discord server didn't work."])]
         :session {:identity user-uuid}}))))

(defn GET-login [req]
  {:html/layout false
   :html/body [auth-html/popup (-> req :params :next)]})

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
