(ns co.gaiwan.compass.util
  (:require
   [clojure.core.protocols :as p]
   [clojure.datafy :as d]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [ring.util.response :as response])
  (:import
   (java.time Instant ZonedDateTime ZoneId)))

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

(defn redirect
  "Returns a Ring response for an HTTP 302 redirect. Status may be
  a key in redirect-status-codes or a numeric code. Defaults to 302"
  ([url]
   (redirect url :found))
  ([url {:keys [status flash push-url?]}]
   (let [url (str (if (vector? url)
                    (str/join "/" url)
                    url))]
     (cond-> {:status  (response/redirect-status-codes status status)
              :headers {"Location" url
                        "HX-Location" url}
              :body    ""}
       flash
       (assoc :flash flash)))))
