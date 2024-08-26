(ns co.gaiwan.compass.repl
  (:require
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.model.user :as u]))

(defn user [name-or-email]
  (db/entity
   (db/q '[:find ?e .
           :in $ ?n-e
           :where
           [?e :public-profile/name ?n]
           [?e :discord/email ?m]
           [(.contains ^String ?n ?n-e)
            ]]
         (db/db)
         name-or-email
         )))

(defn unassign-ticket [user]
  @(db/transact [[:db/retract (:db/id (u/assigned-ticket user)) :tito.ticket/assigned-to (:db/id user)]]))

(comment
  (into {}
        (:tito.ticket/release
         (u/assigned-ticket
          (user "Arne")))))
