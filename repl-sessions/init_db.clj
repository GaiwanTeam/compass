(ns repl-sessions.init-db
  "Prepare some testing data to transact into db for testing"
  (:require
   [co.gaiwan.compass.db :as db]
   [datomic.api :as d]))

;; Utility

(defn query-session []
  (db/q
   '[:find
     [(pull ?e [*]) ...]
     :where
     [?e :session/title "Opening Keynote (TBD)"]]
   (db/db)))

(defn participants []
  (db/q
   '[:find
     [(pull ?e [*]) ...]
     :where
     [?e :user/email]]
   (db/db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;        Test Setup Begin
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; write participants into the database
(db/transact [{:user/email "laurence@gaiwan.co"
               :user/handle "laurence.chen"
               :user/name "Laurence"}
              {:user/email "arne@gaiwan.co"
               :user/handle "sunnyplexus"
               :user/name "Arne"}])

;; get the session eid
(def session-eid (:db/id (first (query-session))))

;; get the eids of participants
(def p-eids (mapv :db/id (participants)))

(def tx-data (mapv (fn [x] {:db/id session-eid
                            :session/participants x}) p-eids))

;; relate the pariticipants to the session
(db/transact tx-data)

;; Simulate the entity access in the frontend
(def session-entity (db/entity session-eid))

;; Get the first participant's name from the session entity
(-> session-entity
    :session/participants
    first
    :user/name)

;; Demonstrate the behaviors of Datomic Entity

(type session-entity)
;; => co.gaiwan.compass.db.munged-entity

(type (-> session-entity
          :session/participants
          first))
;; => datomic.query.EntityMap

(-> session-eid
    db/entity
    :session/type
    db/entity
    :session.type/color)
;; => "var(--talk-color)"

(-> session-eid
    db/entity
    :session/type
    db/entity
    (merge {}))

;; => {:db/ident :session.type/keynote,
;;     :session.type/name "Keynote",
;;     :session.type/color "var(--talk-color)"}

(db/pull '[* {:session/type [*]
              :session/location [*]}] session-eid)
