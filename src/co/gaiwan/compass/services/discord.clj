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
  [user-id {{:keys [tito.release/slug]} :tito.ticket/release :as _ticket}]
  (let [role-endpoint (str "/guilds/" (config/value :discord/server-id) "/members/" user-id "/roles/")]
    (->> [(discord-bot-request :put (str role-endpoint (config/value :discord/ticket-holder-role)))
          (when-let [special-role (get (config/value :discord/ticket-roles) slug)]
            (discord-bot-request :put (str role-endpoint special-role)))]
         (map #(get % :status 200))
         (every? #(= (quot % 100) 2)))))

;; NOTE: we originally wanted to use the linked roles Discord feature, but the UX of that turned out to be crap.
;; So now we don't use it anymore and assign configured roles directly
(comment
  (def role-connection-metadata
    "Metadata field for roles linked to ticket type/tier. Type/tier is represented by an integer."
    [{:type 7 ;; BOOLEAN_EQUAL
      :key "tito_ticket_holder"
      :name "Ticket holder?"
      :description "User must be a ticket holder"}
     {:type 3 ;; INTEGER_EQUAL
      :key "tito_release_slug_idx"
      :name "Tito release type"
      :description
      (->> (config/value :discord/ticket-roles)
           (map-indexed (fn [i v] (str i " = " v)))
           (str/join ", ")
           (str "User must have a specific type of ticket: "))}])

  (defn update-role-connection
    "Update a user's Discord role connection according to tito ticket information.

  `token` is an OAuth2 token with role_connections.write scope.
  `ticket` is a ticket map containing the associated release or `nil`, if the user should not be considered a ticket holder (anymore)."
    [token {{:keys [tito.release/slug]} :tito.ticket/release :as ticket}]
    (hato/put
     (str discord-api-endpoint "/users/@me/applications/" (:id (get-application)) "/role-connection")
     {:as :auto
      :oauth-token token
      :content-type :json
      :form-params
      {:platform_name "Compass"
       :metadata
       ;; Find if release slug is among the "special" role slugs declared in the config
       (let [slug-idx (->> (config/value :discord/ticket-roles)
                           (map-indexed vector)
                           (some (fn [[i v]] (when (= v slug) i))))]
         ;; Ticket holder property is "true" if  user _has_ ticket
         (cond-> {"tito_ticket_holder" (if (some? ticket) 1 0)}
           ;; slug idx is set if user has ticket with special release type
           slug-idx (assoc "tito_release_slug_idx" slug-idx)))}})))

(comment
  ;; Register role connection metadata
  (discord-bot-request :put (str "/applications/" (:id (get-application)) "/role-connections/metadata") role-connection-metadata)
  ;; get user role connection
  (hato/get (str discord-api-endpoint "/users/@me/applications/" (:id (get-application)) "/role-connection")
            {:oauth-token ""
             :as :auto}))
