(ns co.gaiwan.compass.db.queries
  "Database queries, to be used in routes"
  (:require
   [co.gaiwan.compass.db :as db]))

#_(set! *print-namespace-maps* false)

(defn ?resolve-ident
  "Maybe resolve ident

  We gave some entities idents, which messes up `db/entity`, which thinks you're
  dealing with an enum, and 'helpfully' returns a keyword."
  [o]
  (if (keyword? o)
    (db/entity o)
    o))

(defn session [id]
  (let [e (db/entity id)]
    (-> (into {:db/id (:db/id e)} e)
        (update :session/type ?resolve-ident)
        (update :session/location ?resolve-ident)
        (assoc :session/signup-count (count (:session/participants e))))))

(defn all-sessions
  []
  (sort-by
   :session/time
   (map session
        (db/q
         '[:find
           [?e ...]
           :where
           [?e :session/title]]
         (db/db)))))

(defn all-users []
  (sort-by
   :public-profile/name
   (db/q
    '[:find
      [(pull ?e [*
                 {:tito.ticket/_assigned-to [*]}]) ...]
      :where
      [?e :public-profile/name]]
    (db/db))))

(defn public-links [user-eid]
  (map db/entity
       (db/q
        '[:find [?l ...]
          :in $ ?u
          :where
          [?u :public-profile/links ?l]]
        (db/db)
        user-eid)))

(defn private-links [user-eid]
  (map db/entity
       (db/q
        '[:find [?l ...]
          :in $ ?u
          :where
          [?u :public-profile/links ?l]]
        (db/db)
        user-eid)))

(defn all-session-types []
  (db/q
   '[:find [(pull ?t [*]) ...]
     :where
     [?t :session.type/name]]
   (db/db)))

(defn all-locations []
  (db/q
   '[:find [(pull ?t [*]) ...]
     :where
     [?t :location/name]]
   (db/db)))
