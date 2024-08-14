(ns repl-sessions.user-group
  "Prepare some testing data to transact into db for testing"
  (:require
   [co.gaiwan.compass.db :as db]
   [datomic.api :as d]))

(defn find-user-eid [email]
  (db/q
   '[:find ?e .
     :in $ ?m
     :where
     [?e :user/email ?m]]
   (db/db) email))

;; Init the orga group
;; Add orga user by email
(let [{:keys [tempids]} @(db/transact [{:db/id "user-group"
                                        :user-group/orga true
                                        :user-group/user-count 0}])
      ug-eid (get tempids "user-group")
      u-eid (find-user-eid "humorless@gmail.com")]
  @(db/transact [[:db/cas ug-eid :user-group/user-count 0 1]
                 [:db/add ug-eid :user-group/users u-eid]]))

;; Double check the user-group just created
(db/q
 '[:find [(pull ?e [*]) ...]
   :where
   [?e :user-group/users]]
 (db/db))

(def user-entity
  (db/entity
   (find-user-eid "humorless@gmail.com")))

(type user-entity)

(->
 (:user-group/_users user-entity)
 first
 :user-group/orga)
