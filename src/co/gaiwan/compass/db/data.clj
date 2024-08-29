(ns co.gaiwan.compass.db.data
  "Static data that gets imported at boot"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.model.assets :as assets]))

(require 'java-time-literals.core)

(defn locations []
  [{:location/name "Het Depot"}
   {:location/name "Hal 5"}])

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
  (map
   (fn [s]
     (update s :session/image #(assets/download-image %)))
   (read-string (slurp (io/resource "compass/schedule.edn")))))

