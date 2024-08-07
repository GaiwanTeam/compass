(ns co.gaiwan.compass.db.schema)

;; bigdec | bigint | boolean | bytes | double | float | instant |
;; keyword | long | ref | string | symbol | tuple | uuid | uri

(def schema
  [[:user/uuid :uuid "Unique user identifier" :identity]
   [:user/email :string "User email" :identity]
   [:user/name :string "User name, e.g. 'Arne'"]
   [:user/handle :string "User handle, e.g. 'sunnyplexus'"]
   [:user/title :string "User's job title or any description, e.g. 'CEO of Gaiwan'"]

   [:discord/id :string "Unique user id on discord, a 'snowflake', i.e. uint64 encoded as string"]
   [:discord/access-token :string "Discord OAuth2 access-token"]
   [:discord/expires-at :instant "Expiration timestamp for the OAuth2 token"]
   [:discord/refresh-token :string "Discord OAuth2 refresh-token"]

   [:session/title :string "Title of the talk/workshop/activity"]
   [:session/subtitle :string "Subtitle of the session, for talks/workshops = speaker names"]
   [:session/description :string "Full description or abstract"]
   [:session/type :ref "Type of the session"]
   [:session/organized :ref "User who organizes this session"]
   [:session/time :instant "Time the session starts"]
   [:session/duration :string "Duration of the session in ISO interval notation"]
   [:session/location :ref "Where does the session take place"]
   [:session/image :string "Image URL, either absolute, or relative to compass root"]
   [:session/capacity :long "Number of people that are able to join this session"]
   [:session/signup-count :long "Number of people that are currently signing up this session"]
   [:session/ticket-required? :boolean "If this session requires a ticket"]
   [:session/published? :boolean "If this session is published/visible?"]
   [:session/participants :ref "reference points to the user" :many]

   [:session.type/name :string "Type of session, e.g. talk, activity"]
   [:session.type/color :string "CSS color or var reference used for rendering"]

   [:location/name :string "Name of the location" :identity]
   [:oauth/state-id :uuid "State parameter passed along with the oauth flow" :identity]
   [:oauth/redirect-url :string "Location to redirect to after login"]])

(defn inflate-schema [s]
  (for [[ident type doc & flags] s]
    (cond-> {:db/ident     ident
             :db/valueType (keyword "db.type" (name type))
             :db/doc       doc
             :db/cardinality (if (some #{:many} flags)
                               :db.cardinality/many
                               :db.cardinality/one)}
      (some #{:identity} flags)
      (assoc :db/unique :db.unique/identity)
      (some #{:value} flags)
      (assoc :db/unique :db.unique/value)
      (some #{:component} flags)
      (assoc :db/isComponent true))))

(defn schema-tx []
  (inflate-schema schema))

(comment
  (user/reset))
