(ns co.gaiwan.compass.db.queries
  "Database queries, to be used in routes"
  (:require
   [co.gaiwan.compass.db :as db]))

(defn all-sessions
  []
  (sort-by :session/time
           (db/q
            '[:find
              [(pull ?e [* {:session/type [*]
                            :session/location [*]}]) ...]
              :where
              [?e :session/title]]
            (db/db))))

(defn all-users []
  )
