(ns co.gaiwan.compass.html.home
  "Front page views and components"
  {:ornament/prefix "home-"}
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.html.sessions :as sessions]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.util :as util]
   [clojure.datafy :as df]
   [lambdaisland.ornament :as o]))

(defn filters-hidden []
  [:section#filters
   [:button
    {:hx-get "/show-filters"
     :hx-trigger "click"
     :hx-swap "outerHTML"
     :hx-target "#filters"
     :hx-select "#filters"}
    "Open Session Filters"]])

(defn filters-showed []
  [:section#filters
   [:button
    {:hx-get "/hide-filters"
     :hx-trigger "click"
     :hx-swap "outerHTML"
     :hx-target "#filters"
     :hx-select "#filters"}
    "Close Session Filters"]
   [:div
    [:label "Starred"]
    [:input {:type "checkbox"}]]
   [:div
    [:label "Participating"]
    [:input {:type "checkbox"}]]
   [:div
    [:label "Spots available"]
    [:input {:type "checkbox"}]]
   [:div
    [:label "Type"]
    [:select
     [:option {:value "talk"} "talk"]
     [:option {:value "workshop"} "workshop"]
     [:option {:value "office-hours"} "office-hours"]
     [:option {:value "sessions"} "session"]]]
   [:div
    [:label "Location"]
    [:select
     [:option {:value "depot"} "depot"]
     [:option {:value "hal5"} "hal5"]]]])

(o/defstyled home :div
  [sessions/session-card :mb-3]
  ([{:keys [user sessions]}]
   [:<>
    [filters-hidden]
    [:main
     (for [session sessions]
       [sessions/session-card session])]]))

(comment
  [sessions/session-card (sessions/rand-session)]

;; Not in use now
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
                  hour minute]} (df/datafy time)
          day-of-week-str (week-day-str day-of-week)
          date-str (format "%02d.%02d" day-of-month month)
          time-str (format "%02d:%02d" hour minute)]
      {:title name
       :speaker speaker
       :type type-keyword
       :organized organized
       :day day-of-week-str
       :date date-str
       :time time-str
       :location location-name})))
