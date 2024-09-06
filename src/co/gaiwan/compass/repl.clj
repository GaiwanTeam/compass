(ns co.gaiwan.compass.repl
  "REPL utility functions for quick maintenance tasks

  See also `bin/dev prod-repl`
  "
  (:require
   [co.gaiwan.compass.db :as db :refer :all]
   [co.gaiwan.compass.model.user :as u]))

(comment
  (require 'co.gaiwan.compass.repl)
  (in-ns 'co.gaiwan.compass.repl)
  )

(defn user [name-or-email]
  (db/entity
   (db/q '[:find ?e .
           :in $ ?n-e
           :where
           [?e :public-profile/name ?n]
           [?e :discord/email ?m]
           [(.contains ^String ?n ?n-e)]]
         (db/db)
         name-or-email)))

(defn sessions []
  (map db/entity (db/q '[:find [?e ...]
                         :where
                         [?e :session/title]]
                       (db/db))))

(defn unassign-ticket [user]
  @(db/transact [[:db/retract (:db/id (u/assigned-ticket user)) :tito.ticket/assigned-to (:db/id user)]]))

(comment
  (into {}
        (:tito.ticket/release
         (u/assigned-ticket
          (user "Arne")))))
