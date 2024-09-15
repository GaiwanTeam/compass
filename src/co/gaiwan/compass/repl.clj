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

(:db/id
 (user "Arne"))

{:user/uuid #uuid "ee944d53-0c49-486c-9b4e-a178491673ba", :public-profile/name "Arne", :public-profile/avatar-url "66c1ebd5cfd87056a7fd591c773efe4cfe022304554e3f49988fb7a240010c19.png", :discord/id "758588684177768469", :discord/access-token "2cYNs1YwseOCwjmlkid2r7QiiVIl01", :discord/expires-at #time/zdt "2024-09-21T09:37:58.756+02:00[Europe/Brussels]", :discord/refresh-token "YauvlnohxgWk7XREbfRD02cXsV61xj", :discord/email "arne.brasseur@gmail.com"}
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
