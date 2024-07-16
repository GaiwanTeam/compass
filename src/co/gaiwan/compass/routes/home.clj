(ns co.gaiwan.compass.routes.home
  "Front page routes and business logic"
  (:require
   [co.gaiwan.compass.html.home :as h]
   [co.gaiwan.compass.db :as db]))

(defn all-sessions []
  (db/q
   '[:find
     [(pull ?e [* {:session/type [*]
                   :session/location [*]}]) ...]
     :where
     [?e :session/title]]
   (db/db)))

(defn GET-home [req]
  {:html/head [:title "Conference Compass"]
   :html/body [h/home {:user (:identity req)
                       :sessions (all-sessions)}]})

(defn GET-no-filters [req]
  {:html/head [:title "filter snippets"]
   :html/body
   [:session#filters
    [:button
     {:hx-get "/open-filters"
      :hx-trigger "click"
      :hx-swap "outerHTML"
      :hx-target "#filters"
      :hx-select "#filters"}
     "Open Session Filters"]]})

(defn GET-filters [req]
  {:html/head [:title "filter snippets"]
   :html/body
   [:section#filters
    [:button
     {:hx-get "/close-filters"
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
      [:option {:value "hal5"} "hal5"]]]]})

(defn routes []
  [""
   ["/"
    {:name :index
     :get {:handler GET-home}}]
   ["/open-filters"
    {:get {:handler GET-filters}}]
   ["/close-filters"
    {:get {:handler GET-no-filters}}]])
