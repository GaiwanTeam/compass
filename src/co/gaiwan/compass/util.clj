(ns co.gaiwan.compass.util
  (:import [java.time Instant ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter])
  (:require
   [clojure.pprint :as pprint]))

(defn format-instant
  "Output: 
  
  ```
  {:time #object[java.time.LocalTime],
   :day-of-week #object[java.time.DayOfWeek],
   :month LONG,
   :day-of-month LONG}
  ```"
  [instant]
  (let [zone-id (ZoneId/systemDefault)
        zdt (ZonedDateTime/ofInstant instant zone-id)
        day-of-week (.getDayOfWeek zdt)
        month (.getMonthValue zdt)
        day-of-month (.getDayOfMonth zdt)
        time (.toLocalTime zdt)]
     {:time time
      :day-of-week day-of-week
      :month month
      :day-of-month day-of-month}))

(comment
  (format-instant (Instant/now))
  )

(defn pprint-str [o]
  (with-out-str (pprint/pprint o)))
