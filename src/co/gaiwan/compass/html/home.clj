(ns co.gaiwan.compass.html.home
  "Front page views and components"
  {:ornament/prefix "home-"}
  (:require
   [co.gaiwan.compass.html.sessions :as sessions]
   [java-time.api :as time]
   [lambdaisland.ornament :as o]))

(o/defstyled filters :section#filters
  :flex :flex-wrap :gap-1
  :my-3
  [#{:button :.btn} :font-normal :flex-grow]
  ([all-sessions]
   [:<>
    [:button "Today " (count (filter #(= (time/month-day)
                                         (time/month-day (:session/time %)))
                                     all-sessions))]
    [:button "All " (count all-sessions)]
    [:button "My Activities"]
    [:button "Created By Me"]
    [:a.btn {:href "/sessions/new" :hx-boost "false"} "Create An Activity"]
    ]))

;; [:div
;;  [:label "Starred"]
;;  [:input {:type "checkbox"}]]
;; [:div
;;  [:label "Participating"]
;;  [:input {:type "checkbox"}]]
;; [:div
;;  [:label "Spots available"]
;;  [:input {:type "checkbox"}]]
;; [:div
;;  [:label "Type"]
;;  [:select
;;   {:name "type"
;;    :hx-include "select[name='location']"
;;    :hx-get "/conf-sessions"
;;    :hx-select "#sessions"
;;    :hx-target "#sessions"}
;;   [:option {:value "all"} "all"]
;;   [:option {:value "talk"} "talk"]
;;   [:option {:value "workshop"} "workshop"]
;;   [:option {:value "keynote"} "keynote"]
;;   [:option {:value "office-hours"} "office-hours"]
;;   [:option {:value "session"} "session"]]]
;; [:div
;;  [:label "Location"]
;;  [:select
;;   {:name "location"
;;    :hx-include "select[name='type']"
;;    :hx-get "/conf-sessions"
;;    :hx-select "#sessions"
;;    :hx-target "#sessions"}
;;   [:option {:value "all"} "all"]
;;   [:option {:value "depot"} "Het Depot"]
;;   [:option {:value "hal5"} "Hal 5"]]]

(o/defstyled home :div
  ([{:keys [user sessions]}]
   [:<>
    [filters sessions]
    [sessions/session-list {:user user
                            :sessions sessions}]]))
