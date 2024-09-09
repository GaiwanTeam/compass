(ns co.gaiwan.compass.db.migrations
  (:require [co.gaiwan.compass.db.data :as data]))

(def all
  [{:label :add-locations
    :tx-data (data/locations)}

   {:label :add-session-types
    :tx-data (data/session-types)}

   {:label :add-initial-schedule
    :tx-data (data/load-schedule "compass/schedule.edn")}

   {:label :add-live-set
    :tx-data [{:session.type/name  "Live Set"
               :session.type/color "var(--workshop-color)"
               :db/ident           :session.type/live-set}]}

   {:label :add-updated-schedule
    :tx-data (data/load-schedule "compass/schedule_20240909.edn")}

   {:label :update-locations
    :tx-data
    [[:db/retractEntity :location.type/depot-main-stage]
     [:db/retractEntity :location.type/hal5-zone-a]
     [:db/retractEntity :location.type/hal5-zone-b]
     [:db/retractEntity :location.type/hal5-hoc-cafe]
     {:db/ident :location.type/hal5
      :location/name "Hal 5 - Workshop Zone"}
     {:location/name "Hal 5 - Community Space"}]}
   ])
