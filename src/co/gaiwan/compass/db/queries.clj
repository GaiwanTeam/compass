(ns co.gaiwan.compass.db.queries
  "Database queries, to be used in routes"
  (:require
   [co.gaiwan.compass.db :as db]))

(defn all-sessions
  []
  (sort-by
   :session/time
   (db/q
    '[:find
      [(pull ?e [* {:session/type [*]
                    :session/location [*]}]) ...]
      :where
      [?e :session/title]]
    (db/db))))

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
