(ns co.gaiwan.compass.html.filters
  {:ornament/prefix "filters-"}
  (:require
   [co.gaiwan.compass.html.components :as c]
   [lambdaisland.ornament :as o]))

(def filter-props
  {:hx-put "/filters"
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
   [:<> {:hx-get     "/filters"
         :hx-trigger "filters-updated from:body"
         :hx-swap    "outerHTML"}

    [filter-group
     {:value   (:day state :any-day)
      :name    "day"
      :options [[:any-day "All Days"]
                [:today "Today"]
                [:tomorrow "Tomorrow"]]}]

    [filter-group
     {:value   (:type state :all-types)
      :name    "type"
      :options [[:all-types "All Types"]
                [:talks "Talks"]
                [:activities "Activities"]]}]

    (for [[id caption] [[:my-activities "My Activities"]
                        [:include-past "Include Past"]
                        [:spots-available "Spots Available"]]]
      [filter-btn {:id       id
                   :caption  caption
                   :checked? (get state id)}]
      )
    #_[:a.btn {:href "/sessions/new" :hx-boost "false"} "Create An Activity"]
    ]))
