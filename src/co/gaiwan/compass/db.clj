(ns co.gaiwan.compass.db
  (:require
   [clojure.walk :as walk]
   [co.gaiwan.compass.db.data :as data]
   [co.gaiwan.compass.db.schema :as schema]
   [datomic.api :as d]
   [integrant.core :as ig]
   [integrant.repl.state :as state]
   [io.pedestal.log :as log]
   [potemkin.collections :as po-coll]))

(declare transact)

(def event-time-zone (java.time.ZoneId/of "Europe/Brussels"))

(defmethod ig/init-key :compass/db [_ {:keys [url]}]
  (d/create-database url)
  (let [conn (d/connect url)]
    @(transact conn (schema/schema-tx))
    @(transact conn (data/locations))
    @(transact conn (data/session-types))
    @(transact conn (data/schedule))
    conn))

(defmethod ig/halt-key! :compass/db [_ conn])

(defn conn []
  (:compass/db state/system))

(defn db []
  (d/db (conn)))

(declare munge-from-db ->munged-entity)

(po-coll/def-map-type munged-entity [e]
  (get [this k default-value] (munge-from-db (get e k default-value)))
  (assoc [this k v] (->munged-entity (assoc e k v)))
  (dissoc [this k] (->munged-entity (dissoc e k)))
  (keys [this] (keys e))
  (meta [this] (meta e))
  (with-meta [this m] (->munged-entity (with-meta e m))))

(defn munge-to-db [value]
  (cond
    (instance? java.time.Instant value)
    (java.util.Date/from value)

    (instance? java.time.ZonedDateTime value)
    (java.util.Date/from (.toInstant ^java.time.ZonedDateTime value))

    :else
    value))

(defn munge-from-db [value]
  (cond
    (instance? java.util.Date value)
    (java.time.ZonedDateTime/ofInstant
     (.toInstant ^java.util.Date value)
     event-time-zone)

    (instance? datomic.query.EntityMap value)
    (->munged-entity value)

    :else
    value))

(defn pull [selector id]
  (walk/postwalk munge-from-db (d/pull (db) selector id)))

(defn transact
  ([tx-data]
   (transact (conn) tx-data))
  ([conn tx-data]
   (log/trace :datomic/transacting tx-data)
   (d/transact conn (walk/postwalk munge-to-db tx-data))))

(defn q [& args]
  (walk/postwalk munge-from-db (apply d/q args)))

(defn entity [lookup]
  (->munged-entity (d/entity (db) lookup)))

(comment
  ;; reload schema and data
  (user/reset))
