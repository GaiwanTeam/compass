(ns co.gaiwan.compass.db.migrations
  (:require
   [co.gaiwan.compass.db.data :as data]
   [datomic.api :as d]))

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

   {:label :cap-capacity-fn
    :tx-data
    [{:db/ident :compass.fn/cap-capacity
      :db/fn (d/function {:lang "clojure"
                          :params '[db cap]
                          :code '(for [sid (datomic.api/q '[:find [?e ...]
                                                            :in $ ?cap
                                                            :where
                                                            [?e :session/capacity ?c]
                                                            [(< ?cap ?c)]]
                                                          db cap)]
                                   [:db/add sid :session/capacity cap])})}]}

   {:label :cap-capacity-250
    :tx-data
    [[:compass.fn/cap-capacity 250]]}
   ])
