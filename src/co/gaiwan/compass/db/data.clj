(ns co.gaiwan.compass.db.data
  "Static data that gets imported at boot"
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(require 'java-time-literals.core)

(defn locations []
  [{:location/name "Het Depot"
    :db/ident      :location.type/depot}
   {:location/name "Het Depot - main stage"
    :db/ident      :location.type/depot-main-stage}
   {:location/name "Het Depot - Bar"
    :db/ident      :location.type/depot-bar}
   {:location/name "Hal 5"
    :db/ident      :location.type/hal5}
   {:location/name "Hal 5 - zone A"
    :db/ident      :location.type/hal5-zone-a}
   {:location/name "Hal 5 - zone B"
    :db/ident      :location.type/hal5-zone-b}
   {:location/name "Hal 5 - HoC Café"
    :db/ident      :location.type/hal5-hoc-cafe}
   {:location/name "Hal 5 - Foodcourt"
    :db/ident      :location.type/hal5-foodcourt}
   {:location/name "Hal 5 - park"
    :db/ident      :location.type/hal5-park}
   {:location/name "Hal 5 - outside seating"
    :db/ident      :location.type/hal5-outside-seating}
   {:location/name "Hal 5 - long table"
    :db/ident      :location.type/hal5-long-table}])

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

