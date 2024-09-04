(ns co.gaiwan.compass.html.filters
  {:ornament/prefix "filters-"}
  (:require
   [co.gaiwan.compass.html.components :as c]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [lambdaisland.ornament :as o]))

(def filter-props
  {:hx-put (url-for :filters/index)
   :hx-include "#filters input"
   :hx-swap "none"})

(defn filter-btn [{:keys [id caption checked?]}]
  [c/toggle-button (assoc filter-props
                          :id (name id)
                          :checked? checked?) caption])

(defn filter-group [props]
  [c/toggle-group (merge filter-props props)])

(o/defstyled filter-section :section#filters
  :flex :flex-wrap :gap-1
  :my-3
  [#{:button :.btn c/toggle-button} :font-normal :flex-grow]
  ([state]
   [:<> {:hx-get     (url-for :filters/index)
         :hx-trigger "filters-updated from:body"
         :hx-swap    "outerHTML"}

    [filter-group
     {:value   (:type state :all-types)
      :name    "type"
      :options [[:all-types "All Types"]
                [:talks "Talks"]
                [:activities "Activities"]]}]

    [filter-group
     {:value   (:day state :any-day)
      :name    "day"
      :options [[:any-day "All Days"]
                [:today "Today"]
                [:tomorrow "Tomorrow"]]}]

    [filter-btn
     {:id :include-past,
      :caption "Include Past",
      :checked? (get state :include-past)}]

    [filter-btn
     {:id :my-activities,
      :caption "My Activities",
      :checked? (get state :my-activities)}]

    [filter-btn
     {:id :spots-available,
      :caption "Spots Available",
      :checked? (get state :spots-available)}]
    #_[:a.btn {:href (url-for :session/new) :hx-boost "false"} "Create An Activity"]]))
