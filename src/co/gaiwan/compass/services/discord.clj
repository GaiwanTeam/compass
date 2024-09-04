(ns co.gaiwan.compass.services.discord
  "Discord API access"
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [hato.client :as hato]
   [io.pedestal.log :as log]
   [co.gaiwan.compass.db :as db]))

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
     (log/trace :discord/request (-> response :request (update :headers dissoc "Authorization")) :discord/response (dissoc response :request))
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

(defn create-session-thread
  "Create private thread for session in channel configured by `:discord/session-channel-id`."
  [session]
  (let [{:keys [status] thread :body :as response}
        (discord-bot-request
         :post
         (str "/channels/" (config/value :discord/session-channel-id) "/threads")
         {:name (->> session :session/title (take 100) str/join)
          :type 12 ;; PRIVATE_THREAD
          :invitable true})]
    (case status
      201
      (do
        (db/transact [[:db/add (:db/id session) :session/thread-id (:id thread)]])
        thread)
      (log/error :discord/thread-create-failed "Failed to create session thread"
                 :session session
                 :response (dissoc response :request)))))

(defn get-session-thread
  "Get existing thread for session (or `nil`)."
  [session]
  (let [id (:session/thread-id session)
        {channel :body :keys [status] :as response} (discord-bot-request :get (str "/channels/" id))]
    (case status
      ;; thread already exists
      200 channel
      ;; thread does not exist
      404 nil
      ;; something else?
      (log/error
        :discord/thread-fetch-failed (str "Unhandled status code " status " for getting session thread")
        :session session
        :response (dissoc response :request)))))

(defn user-mention
  "Discord ID -> text that pings user"
  [user-id]
  (str "<@" user-id ">"))

(defn send-session-thread-message
  "Send a message to the session thread belonging to `session-id`.

  Returns `nil`.
  A `message` containing @everyone will send a notification to all thread members.
  A `message` mentioning one or more individual users ([[user-mention]]) will notify and add those users to the thread.
  Will fail if there is no session thread (yet) or if the sending the message fails.
  In both cases, an error message is logged."
  [session-id message]
  (if-let [{:keys [id] :as _thread} (get-session-thread (db/entity session-id))]
    (let [{:keys [status] :as response}
          (discord-bot-request
           :post
           (str "/channels/" id "/messages")
           {:content message
            :allowed_mentions {:parse ["users" "everyone"]}})]
      (when-not (= status 200)
        (log/error
          :discord/message-send-fail "Failed to send message to session thread"
          :session (db/entity session-id)
          :response (dissoc response :request))))
    (log/error :discord/missing-session-thread "Tried to send message to session thread that doesn't exist")))

(defn add-to-session-thread
  "Add user with `user-id` to session thread of session with `session-id`.

  Returns `nil`. In case of failure, an error message is logged."
  [session-id user-id]
  (let [session (db/entity session-id)
        {:keys [status] :as response} (discord-bot-request :put (str "/channels/" (:session/thread-id session) "/thread-members/" user-id))]
    (when-not (= status 204)
      (log/error
        :discord/thread-member-add-failed "Failed to add member to session thread"
        :session session
        :response (dissoc response :request)))))
