(ns co.gaiwan.compass.http.oauth
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [lambdaisland.uri :as uri]))

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

