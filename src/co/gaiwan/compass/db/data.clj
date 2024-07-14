(ns co.gaiwan.compass.db.data
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(require 'java-time-literals.core)

(defn locations []
  [{:location/name "Het Depot"
    :db/ident      :location.type/depot}
   {:location/name "Hal 5"
    :db/ident      :location.type/hal5}
   ])

(defn session-types []
  [{:session.type/name  "Talk"
    :session.type/color "var(--talk-color)"
    :db/ident           :session.type/talk}
   {:session.type/name  "Workshop"
    :session.type/color "var(--workshop-color)"
    :db/ident           :session.type/workshop}
   {:session.type/name  "Session"
    :session.type/color "var(--workshop-color)"
    :db/ident           :session.type/session}
   {:session.type/name  "Office Hours"
    :session.type/color "var(--office-hours-color)"
    :db/ident           :session.type/office-hours}
   {:session.type/name  "Keynote"
    :session.type/color "var(--talk-color)"
    :db/ident           :session.type/keynote}
   {:session.type/name  "Activity"
    :session.type/color "var(--activity-color)"
    :db/ident           :session.type/activity}])

(defn schedule []
  (read-string (slurp (io/resource "compass/schedule.edn"))))

