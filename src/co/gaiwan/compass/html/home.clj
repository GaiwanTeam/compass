(ns co.gaiwan.compass.html.home
  "The UI of the compass system"
  {:ornament/prefix "home-"}
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.html.sessions :as sessions]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.util :as util]
   [clojure.datafy :as df]
   [lambdaisland.ornament :as o]))

(def session-query
  '[:find (pull ?e [*]) ?type-keyword ?location-name
    :where [?e :session/name]
    [?e :session/location ?l]
    [?l :location/name ?location-name]
    [?e :session/type ?t]
    [?t :db/ident ?type-keyword]])

(defn week-day-str [day]
  (let [week-days ["Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday"]]
    (nth week-days (dec day))))

(defn session-model
  "session data from database to frontend
  
   input `x` is a vecotr of 
         `[session-graph type-keyword location-name]`"
  [v]
  (let [[session-graph type-keyword location-name] v
        {:session/keys [time speaker name capacity organized
                        duration]} session-graph
        {:keys [day-of-week month day-of-month
                time]} (df/datafy time)
        day-of-week-str (week-day-str day-of-week)
        time-str (format "%02d.%02d" (.getHour time) (.getMinute time))
        date-str (format "%02d.%02d" month day-of-month)]
    {:title name
     :speaker speaker
     :type type-keyword
     :organized organized
     :day day-of-week-str
     :date date-str
     :time time-str
     :location location-name}))

(o/defstyled home :div
  [sessions/session-card :mb-3]
  ([user]
   [:<>
    [:main
     ;; replace 
     ;; `[sessions/session-card (sessions/rand-session)]`
     ;; with the following let block
     (let [session-data (db/q session-query (db/db))]
       (map
        (fn [x]
          [sessions/session-card
           (session-model x)])
        session-data))]]))

(comment
  [sessions/session-card (sessions/rand-session)]
  [sessions/session-card
   (session-model
    (first (db/q session-query (db/db))))])
