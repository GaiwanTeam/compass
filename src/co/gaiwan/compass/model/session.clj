(ns co.gaiwan.compass.model.session)

(defn participating? [session user]
  (some (comp #{(:db/id user)} :db/id)
        (:session/participants session)))
