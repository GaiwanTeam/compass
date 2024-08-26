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

(comment
  (into {}
        (:tito.ticket/release
         (u/assigned-ticket
          (user "Arne")))))
