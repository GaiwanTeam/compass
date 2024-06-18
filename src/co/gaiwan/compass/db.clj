(ns co.gaiwan.compass.db
  (:require
   [clojure.walk :as walk]
   [datomic.api :as d]
   [integrant.core :as ig]
   [integrant.repl.state :as state]))

;; bigdec | bigint | boolean | bytes | double | float | instant |
;; keyword | long | ref | string | symbol | tuple | uuid | uri

(def enums
  [:session/talk
   :session/workshop
   :session/office-hours
   :session/activity])

(def schema
  [[:user/uuid :uuid "Unique user identifier" :identity]
   [:user/email :string "User email" :identity]
   [:user/name :string "User name, e.g. 'Arne'"]
   [:user/handle :string "User handle, e.g. 'sunnyplexus'"]

   [:discord/id :string "Unique user id on discord, a 'snowflake', i.e. uint64 encoded as string"]
   [:discord/access-token :string "Discord OAuth2 access-token"]
   [:discord/expires-at :instant "Expiration timestamp for the OAuth2 token"]
   [:discord/refresh-token :string "Discord OAuth2 refresh-token"]

   [:session/name :string "Title of the talk/workshop/activity"]
   [:session/time :instant "Time the session starts"]
   [:session/duration :string "Duration of the session in ISO interval notation"]
   [:session/location :ref "Where does the session take place"]
   [:session/capacity :long "Number of people that are able to join this session"]

   [:location/name :string "Name of the location"]])

(defn inflate-enums [e]
  (map #(do {:db/ident %}) e))

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

(defmethod ig/init-key :compass/db [_ {:keys [url]}]
  (d/create-database url)
  (doto (d/connect url)
    (d/transact (concat (inflate-enums enums)
                        (inflate-schema schema)))))

(defmethod ig/halt-key! :compass/db [_ conn]
  )

(defn conn []
  (:compass/db state/system))

(defn db []
  (d/db (conn)))

(defn munge-to-db [value]
  (cond
    (instance? java.time.Instant value)
    (java.util.Date/from value)

    :else
    value))

(defn munge-from-db [value]
  (cond
    (instance? java.util.Date value)
    (.toInstant ^java.util.Date value)

    :else
    value))

(defn transact [tx-data]
  (d/transact (conn) (walk/postwalk munge-to-db tx-data)))

(defn q [& args]
  (walk/postwalk munge-from-db (apply d/q args)))

(comment
  (transact (concat (inflate-enums enums)
                    (inflate-schema schema)))
  (q
   '[:find (pull ?e [*])
     :where [?e :db/ident :user/uuid]]
   (db))
  (q
   '[:find (pull ?e [*])
     :where [?e :user/uuid]]
   (db))


  (d/transact (conn) (inflate-schema schema))
  )

(clojure.reflect/reflect java.time.Instant)

[
 (java.time.Instant/now)
 (java.util.Date/from (java.time.Instant/now))]
