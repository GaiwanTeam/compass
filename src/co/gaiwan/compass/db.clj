(ns co.gaiwan.compass.db
  (:require
   [datomic.api :as d]
   [integrant.core :as ig]
   [integrant.repl.state :as state]))

;; bigdec | bigint | boolean | bytes | double | float | instant |
;; keyword | long | ref | string | symbol | tuple | uuid | uri

(def schema
  [[:user/uuid :uuid "Unique user identifier" :identity]
   [:user/email :string "User email" :identity]
   [:user/name :string "User name, e.g. 'Arne'"]
   [:user/handle :string "User handle, e.g. 'sunnyplexus'"]
   [:discord/access-token :string "Discord OAuth2 access-token"]
   [:discord/expires-at :instant "Expiration timestamp for the OAuth2 token"]
   [:discord/refresh-token :string "Discord OAuth2 refresh-token"]])

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
    (d/transact (inflate-schema schema))))

(defmethod ig/halt-key! :compass/db [_ conn]
  )

(defn conn []
  (:compass/db state/system))

(comment
  (d/transact (user/conn) (inflate-schema schema))
  (d/q
   '[:find (pull ?e [*])
     :where [?e :db/ident :user/uuid]]
   (d/db
    (user/conn)))


  (d/transact (conn) (inflate-schema schema))
  )
