(ns co.gaiwan.compass.db.queries
  "Database queries, to be used in routes"
  (:require
   [co.gaiwan.compass.db :as db]))

#_(set! *print-namespace-maps* false)

(defn session [id]
  (let [e (db/entity id)]
    (-> (into {:db/id (:db/id e)} e)
        (update :session/type db/entity)
        (update :session/location db/entity)
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

(defn all-links [user-eid]
  (sort-by
   :db/id
   (db/q
    '[:find [(pull ?l [*
                       {:public-profile/_links [:db/id]}
                       {:private-profile/_links [:db/id]}]) ...]
      :in $ ?u
      :where
      [?l :profile-link/user ?u]]
    (db/db) user-eid)))

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
