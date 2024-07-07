(ns co.gaiwan.compass.util
  (:import [java.time Instant ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter])
  (:require
   [clojure.core.protocols :as p]
   [clojure.datafy :as d]
   [clojure.pprint :as pprint]))

(defn datafy-instant
  "Output: 
   ```
   {:hour LONG,
    :minute LONG,
    :day-of-week LONG,
    :month LONG,
    :day-of-month LONG}
   ```"
  [instant]
  (let [zone-id (ZoneId/systemDefault)
        zdt (ZonedDateTime/ofInstant instant zone-id)
        day-of-week (.getValue (.getDayOfWeek zdt))
        month (.getMonthValue zdt)
        day-of-month (.getDayOfMonth zdt)
        time (.toLocalTime zdt)]
    {:hour (.getHour time)
     :minute (.getMinute time)
     :day-of-week day-of-week
     :month month
     :day-of-month day-of-month}))

(extend-protocol p/Datafiable
  Instant
  (datafy [d]
    (datafy-instant d)))

(comment
  (d/datafy (Instant/now)))

(defn pprint-str [o]
  (with-out-str (pprint/pprint o)))
