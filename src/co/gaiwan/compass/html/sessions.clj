(ns co.gaiwan.compass.html.sessions
  "Views and components (hiccup/ornament) related to sessions"
  {:ornament/prefix "sessions-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [java-time.api :as time]
   [lambdaisland.ornament :as o]))

(o/defprop --arc-degrees "240deg")
(o/defprop --arc-thickness "40px")
(o/defprop --arc-color --lime-5)

(o/defstyled arc :div
  "Partial circle arc, clockwise. Expects arc (a value in CSS `deg` units) and
  thickness to be passed as props or set as css vars by a parent element."
  {:aspect-ratio  --ratio-square
   :padding       --arc-thickness
   :border-radius --radius-round
   :background    --arc-color
   :mask          (str "linear-gradient(#0000 0 0) content-box intersect, conic-gradient(#000 " --arc-degrees ", #0000 0)")})

(o/defstyled capacity-gauge :div
  "Image with an arc around it to indicate how full a session is."
  :aspect-square
  {:position "relative"}
  [:>* {:position "absolute" :top 0 :left 0}]
  [arc :w-full]
  {--arc-thickness "10%"}
  [:.img :w-full
   {:padding --arc-thickness
    #_#_:margin-left "-100%"}
   [:>* :w-full :aspect-square :rounded-full
    {:background-size "contain"}]]
  ([{:keys [capacity image]}]
   [:<> {:style {--arc-degrees (str (* 360.0 capacity) "deg")}}
    [arc {:style {--arc-degrees "360deg"
                  --arc-color "white"}}]
    [arc]

    [:div.img
     [:div
      {:style {:background-image image}}]]]))

(o/defprop --session-type-color)

(o/defstyled session-card :div
  :flex :gap-3
  :surface-2
  :shadow-4
  :boder :border-solid :border-surface-3
  [:.title :font-size-3]
  [:.datetime :flex-col :items-center :justify-center :font-size-3 :font-bold]
  [:.guage :p-2 :self-center]
  [:.type :font-bold
   :p-1
   :text-center
   :small-caps
   {:writing-mode "vertical-lr"
    :transform "rotate(180deg)"
    :background-color --session-type-color}]
  [capacity-gauge :w-100px]
  [:.details :flex-col :py-2]
  ([{:session/keys [type title subtitle organized time location image] :as s}]
   [:<>
    {:style {--session-type-color (:session.type/color type)}}
    [:div.type (:session.type/name type)]
    [:div.guage
     [capacity-gauge {:capacity (rand)
                      :image (if image
                               (str "url(" image ")")
                               (str "var(--gradient-" (inc (rand-int 7)) ")"))}]]
    [:div.datetime
     [:div
      (subs (str/capitalize (str (time/day-of-week time))) 0 3) " "]
     [:div (time/format "dd.MM" time)]
     [:div (str (time/truncate-to (time/local-time time) :minutes))]]
    [:div.details
     [:h2.title title]
     [:h3.subtitle subtitle]

     [:div.loc (:location/name location)]
     #_[:p.host "Organized by " organized]]]))

;; Create / edit

(o/defstyled session-form :div
  ([params]
   [:<>
    [:h2 "Create Activity"]
    [:form {:method "POST" :action "/sessions"}
     [:div
      [:label {:for "name"} "Activity Name"]
      [:input {:id "name" :name "name" :type "text"}]]

     [:div
      [:label {:for "type"} "Type"]
      [:select {:id "type" :name "type"}
       [:option {:value "activity"} "activity"]]]

     [:div
      [:label {:for "location"} "Location"]
      [:select {:id "location" :name "location"}
       [:option {:value "depot-main-stage"} "Het Depot - main stage"]
       [:option {:value "depot-bar"} "Het Depot - Bar"]
       [:option {:value "hal5-zone-a"} "Hal 5 - zone A"]
       [:option {:value "hal5-zone-b"} "Hal 5 - zone B"]
       [:option {:value "hal5-hoc-cafe"} "Hal 5 - HoC Café"]
       [:option {:value "hal5-foodcourt"} "Hal 5 - Foodcourt"]
       [:option {:value "hal5-park"} "Hal 5 - park"]
       [:option {:value "hal5-outside-seating"} "Hal 5 - outside seating"]
       [:option {:value "hal5-long-table"} "Hal 5 - long table"]]]

     [:div
      [:label {:for "capacity"} "Capacity"]
      [:input {:id "capacity" :name "capacity" :type "number"}]]

     [:div
      [:label {:for "description"} "Description"]
      [:textarea {:id "description" :name "description"}]]

     [:div
      [:label {:for "ticket"} "Requires Ticket?"]
      [:input {:id "ticket" :name "ticket-required?" :type "checkbox"}]]

     [:div
      [:label {:for "published"} "Published/Visible?"]
      [:input {:id "published" :name "published?" :type "checkbox"}]]

     [:input {:type "submit" :value "Create"}]]]))

