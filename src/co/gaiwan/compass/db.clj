(ns co.gaiwan.compass.db
  (:require
   [clojure.walk :as walk]
   [co.gaiwan.compass.db.schema :as schema]
   [datomic.api :as d]
   [integrant.core :as ig]
   [integrant.repl.state :as state]
   [io.pedestal.log :as log]
   [lambdaisland.wagontrain :as wagontrain]
   [potemkin.collections :as po-coll]))

(declare transact)

(def event-time-zone (java.time.ZoneId/of "Europe/Brussels"))

(declare munge-to-db)

(defn migrations []
  (map (fn [m]
         (update
          m :tx-data
          (fn [tx]
            (comp munge-to-db (if (fn? tx)
                                tx
                                (constantly tx))))))
       @(requiring-resolve 'co.gaiwan.compass.db.migrations/all)))

(defmethod ig/init-key :compass/db [_ {:keys [url]}]
  (d/create-database url)
  (let [conn (d/connect url)]
    @(transact conn (concat (schema/schema-tx) wagontrain/schema))
    (wagontrain/migrate! conn (migrations))
    conn))

(defmethod ig/halt-key! :compass/db [_ conn])

(defn conn []
  (:compass/db state/system))

(defn db []
  (d/db (conn)))

(declare munge-1-from-db ->munged-entity)

(po-coll/def-map-type munged-entity [e]
  (get [this k default-value] (munge-1-from-db (get e k default-value)))
  (assoc [this k v] (->munged-entity (assoc e k v)))
  (dissoc [this k] (->munged-entity (dissoc e k)))
  (keys [this] (keys e))
  (meta [this] (meta e))
  (with-meta [this m] (->munged-entity (with-meta e m))))

(defn munge-1-to-db [value]
  (cond
    (instance? java.time.Instant value)
    (java.util.Date/from value)

    (instance? java.time.ZonedDateTime value)
    (java.util.Date/from (.toInstant ^java.time.ZonedDateTime value))

    :else
    value))

(defn munge-to-db [value]
  (if (coll? value)
    (walk/postwalk munge-1-to-db value)
    value))

(defn munge-1-from-db [value]
  (cond
    (instance? java.util.Date value)
    (java.time.ZonedDateTime/ofInstant
     (.toInstant ^java.util.Date value)
     event-time-zone)

    (instance? datomic.query.EntityMap value)
    (->munged-entity value)

    :else
    value))

(defn munge-from-db [value]
  (if (coll? value)
    (walk/postwalk munge-1-from-db value)
    value))

(defn pull [selector id]
  (munge-from-db (d/pull (db) selector id)))

(defn transact
  ([tx-data]
   (transact (conn) tx-data))
  ([conn tx-data]
   (log/trace :datomic/transacting tx-data)
   (d/transact conn (munge-to-db tx-data))))

(defn q [& args]
  (munge-from-db (apply d/q args)))

(defn entity [lookup]
  (when-let [e (d/entity (db) lookup)]
    (->munged-entity e)))

(comment
  ;; reload schema and data
  (user/reset)

  (transact [{:db/id 17592186045437,
              :session/capacity 1}])

  (wagontrain/applied? (conn) :add-locations)

  (wagontrain/rollback! (conn) :add-live-set)
  (wagontrain/rollback! (conn) :add-updated-schedule)
  (wagontrain/migrate! (conn) (migrations))

  )
