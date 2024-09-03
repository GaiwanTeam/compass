(ns co.gaiwan.compass.db.migrations
  (:require [co.gaiwan.compass.db.data :as data]))

(def all
  [{:label :add-locations
    :tx-data (data/locations)}

   {:label :add-session-types
    :tx-data (data/session-types)}

   {:label :add-initial-schedule
    :tx-data (data/schedule)}
   ])
