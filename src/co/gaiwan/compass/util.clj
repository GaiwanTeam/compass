(ns co.gaiwan.compass.util
  "Assorted helper functions, aka the junk drawer.

  Things put here should generally be
  - small, pure functions
  - have little to no dependencies
  - be unopinionated (mechanisms)
  - be usable in multiple contexts
  "
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

(defn to-s [s]
  (if (instance? clojure.lang.Named s)
    (name s)
    (str s)))

(defn dasherize [s]
  (str/replace (to-s s) "_" "-"))

(defn underscorize [s]
  (str/replace (to-s s) "-" "_"))

(defn dasherize-keys [m]
  (when m
    (update-keys m (comp keyword dasherize))))

(defn underscorize-keys [m]
  (when m
    (update-keys m (comp keyword underscorize))))

(defn deep-dasherize-keys [res]
  (cond
    (vector? res) (mapv deep-dasherize-keys res)
    (seq? res)    (map deep-dasherize-keys res)
    (map? res)    (update-vals (dasherize-keys res) deep-dasherize-keys)
    :else         res))

(defn deep-underscorize-keys [res]
  (cond
    (vector? res) (mapv deep-underscorize-keys res)
    (map? res)    (update-vals (underscorize-keys res) deep-underscorize-keys)
    :else         res))

(defn expires-in->instant
  [expires-in]
  (.plusSeconds (Instant/now) (- expires-in 60)))

(defn partition-with-limit
  [limit parts]
  (loop [result []
         current []
         length 0
         [p & rest] parts]
    (let [part-length (count p)
          new-length (+ length part-length)]
      (cond
        (> part-length limit) nil
        (nil? p) (cond-> result (seq current) (conj current))
        (<= new-length limit) (recur result (conj current p) new-length rest)
        :else (recur (conj result current) [] 0 (list* p rest))))))
