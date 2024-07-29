(ns co.gaiwan.compass.model.session
  (:require
   [co.gaiwan.compass.db :as db]))

(defn participating? [session user]
  (some (comp #{(:db/id user)} :db/id)
        (:session/participants session)))

(defn organizing? [organized user]
  (and
   (some? organized)
   (= (:db/id user) (:db/id organized))))

(defn attendee [id]
  (db/entity id))
