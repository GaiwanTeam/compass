(ns co.gaiwan.compass.services.discord
  "Discord API access"
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [hato.client :as hato]
   [io.pedestal.log :as log]))

(def discord-api-endpoint "https://discord.com/api/v10")

(defn bot-auth-headers []
  {"Authorization" (str "Bot " (config/value :discord/bot-token))})

(defn discord-bot-request
  ([method endpoint]
   (discord-bot-request method endpoint nil))
  ([method endpoint body]
   (let [response
         (hato/request
          (cond-> {:method method
                   :url (str discord-api-endpoint endpoint)
                   :throw-exceptions? false
                   :as :auto
                   :headers (bot-auth-headers)}
            body (assoc :content-type :json :form-params body)))]
     (log/trace :discord/request (:request response) :discord/response (dissoc response :request))
     response)))

(defn fetch-user-info [token]
  (:body
   (hato/get (str discord-api-endpoint "/users/@me")
             {:as :auto
              :oauth-token token})))

(defn join-server [token]
  (let [{:keys [id username]} (fetch-user-info token)]
    (discord-bot-request
     :put
     (str "/guilds/" (config/value :discord/server-id) "/members/" id)
     {:access_token token})))

(defn get-application
  []
  (:body (discord-bot-request :get "/applications/@me")))

(defn assign-ticket-roles
  "Takes a Discord user id and a tito ticket map and assigns the appropriate roles to the user in the server.

  Returns true if it succeeded, false if not."
  [user-id {:tito.ticket/keys [release] :as _ticket}]
  (let [{:tito.release/keys [slug]} release
        slug->role-id (config/value :discord/ticket-roles)
        add-role! (fn add-role! [slug]
                    (when-let [role-id (slug->role-id slug)]
                      (discord-bot-request
                       :put
                       (str "/guilds/"  (config/value :discord/server-id)
                            "/members/" user-id
                            "/roles/"   role-id))))]
    (every? (fn [slug]
              (if-let [response (add-role! slug)]
                (<= 200 (:status response) 299)
                true))
            (cond-> [slug]
              (#{"crew"
                 "sponsor"
                 "speaker"
                 "diversity-ticket"
                 "early-bird"
                 "regular-conference"
                 "late-conference"
                 "student"} slug)
              (conj "regular-ticket")))))
