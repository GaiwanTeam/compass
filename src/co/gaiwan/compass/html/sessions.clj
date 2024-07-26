(ns co.gaiwan.compass.html.sessions
  "Views and components (hiccup/ornament) related to sessions"
  {:ornament/prefix "sessions-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [java-time.api :as time]
   [lambdaisland.ornament :as o]
   [markdown-to-hiccup.core :as m]))

(o/defprop --arc-degrees "240deg")
(o/defprop --arc-thickness "30px")
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
  {--arc-thickness "7%"}
  [:.checkmark :hidden :w-full :justify-center :h-full :items-center
   {:font-size "5rem"
    :color --hoc-pink}]
  [:&.checked
   [:.checkmark :flex]
   [:.img {:filter "brightness(50%)"}]]
  [:.img :w-full
   {:padding --arc-thickness
    #_#_:margin-left "-100%"}
   [:>* :w-full :aspect-square :rounded-full
    {:background-size "cover"
     :background-position "50% 50%"}]]
  ([{:keys [capacity image checked?]}]
   [:<> {:class [(when checked? "checked")]
         :style {--arc-degrees (str (* 360.0 capacity) "deg")}}
    [arc {:style {--arc-degrees "360deg"
                  --arc-color "white"}}]
    [arc]
    [:div.img
     [:div
      {:style {:background-image image}}]]
    [:div.checkmark [:div "✓"]]]))

(declare session-card)

(o/defstyled session-actions :nav
  :flex :justify-end :w-full
  :mt-2
  ([session]
   [:<>
    [:button {:hx-post (str "/sessions/" (:db/id session) "/participate")
              :hx-target (str "closest ." session-card)
              :hx-swap "outerHTML"}
     "Participate"]
    [:a {:href (str "/sessions/" (:db/id session))}
     [:button "Details"]]]))

(o/defprop --session-type-color)

(o/defstyled session-card :div
  :flex :gap-1
  :bg-surface-2
  :shadow-2
  :boder :border-solid :border-surface-3
  :text-center
  [:.title :font-size-4 :font-semibold :mb-2]
  [:.subtitle :font-size-3 :font-medium]
  [:.guage :p-2]
  [:.datetime :font-semibold :absolute :top-0 :right-0 :text-right :m-2]
  [:.details :flex-col :w-full :items-center :py-3 :relative]
  [:.type :font-bold
   :p-1
   :text-center
   :small-caps
   {:writing-mode "vertical-lr"
    :transform "rotate(180deg)"
    :background-color --session-type-color}]
  [capacity-gauge :w-100px]
  [session-actions :text-right]
  [:.expansion {:display "none"}]
  [:&.expanded [:.expansion {:display "block"}]]
  ([{:session/keys [type title subtitle organized time location image participants] :as session}
    {user-id :db/id}]
   [:<>
    {:style {--session-type-color (:session.type/color type)}
     :cx-toggle "expanded"
     :cx-target (str "." session-card)}
    [:div.type (:session.type/name type)]

    [:div.details
     [:div.guage
      [capacity-gauge {:capacity (rand)
                       :image (if image
                                (str "url(" image ")")
                                (str "var(--gradient-" (inc (rand-int 7)) ")"))
                       :checked? ((set (map :db/id participants)) user-id)}]]
     [:h2.title title]
     [:h3.subtitle subtitle]
     [:div.datetime
      [:div
       (str (time/truncate-to (time/local-time time) :minutes))]
      [:div
       (subs (str/capitalize (str (time/day-of-week time))) 0 3) " "
       (time/format "dd.MM" time)]]
     [:div.expansion
      [session-actions session]]
     #_[:div.loc (:location/name location)]
     #_[:p.host "Organized by " organized]]]))

(o/defstyled attendee :li
  ([{:user/keys [name email handle uuid]}]
   handle))

(o/defstyled session-detail :div
  :flex :gap-1
  :bg-surface-2
  :shadow-2
  :boder :border-solid :border-surface-3
  :text-center
  [:.title :font-size-4 :font-semibold :mb-2]
  [:.subtitle :font-size-3 :font-medium]
  [:.guage :p-2]
  [:.datetime :font-semibold :absolute :top-0 :right-0 :text-right :m-2]
  [:.details :flex-col :w-full :items-center :py-3 :relative]
  [:.type :font-bold
   :p-1
   :text-center
   :small-caps
   {:writing-mode "vertical-lr"
    :transform "rotate(180deg)"
    :background-color --session-type-color}]
  [capacity-gauge :w-100px]
  ([{:session/keys [type title subtitle organized
                    time location image capacity
                    signup-count description
                    participants] :as session
     :or {participants [{:user/handle "aaa"} {:user/handle "bbb"}]}}]
   [:<>
    {:style {--session-type-color (:session.type/color type)}
     :cx-toggle "expanded"
     :cx-target (str "." session-card)}
    [:div.type (:session.type/name type)]

    [:div.details
     [:div.guage
      [capacity-gauge {:capacity (rand)
                       :image (if image
                                (str "url(" image ")")
                                (str "var(--gradient-" (inc (rand-int 7)) ")"))
                       :checked? (rand-nth [true false])}]]
     [:h2.title title]
     [:h3.subtitle subtitle]
     [:div.datetime
      [:div
       (str (time/truncate-to (time/local-time time) :minutes))]
      [:div
       (subs (str/capitalize (str (time/day-of-week time))) 0 3) " "
       (time/format "dd.MM" time)]]
     [:div.description
      [:div (m/component (m/md->hiccup description))]]
     [:div.location
      [:div "Location "]
      [:div (:location/name location)]]
     [:div.capacity
      [:div "Location capacity:"]
      [:div capacity]]
     [:div.signup-count
      [:div "Current Signup:"]
      [:div signup-count]]
     [:div.participants
      [:div "Participants:"]
      [:ol (map attendee participants)]]
     (when (:session/ticket-required? session)
       [:p "Required Ticket"])
     #_[:p.host "Organized by " organized]
     #_[:p (pr-str session)]]]))

(o/defstyled session-list :main#sessions
  :grid :gap-3
  {:grid-template-columns "repeat(1, 1fr)"}
  [:at-media {:min-width "40rem"} {:grid-template-columns "repeat(2, 1fr)"}]
  [:at-media {:min-width "60rem"} {:grid-template-columns "repeat(3, 1fr)"}]
  [:at-media {:min-width "80rem"} {:grid-template-columns "repeat(4, 1fr)"}]
  ([{:keys [user sessions]}]
   (for [session sessions]
     [session-card session user])))

(o/defrules session-list-cols)

;; Create / edit

(o/defstyled session-form :div
  [:form :grid {:grid-template-columns "10rem 1fr"} :gap-2]
  ([params]
   [:<>
    [:h2 "Create Activity"]
    [:form {:method "POST" :action "/sessions"}
     [:label {:for "title"} "Title"]
     [:input {:id "title" :name "title" :type "text"}]

     [:label {:for "subtitle"} "Subtitle"]
     [:input {:id "subtitle" :name "subtitle" :type "text"}]

     [:label {:for "start-time"} "Start Time"]
     [:input {:id "start-time" :name "start-time" :type "datetime-local"}]

     [:label {:for "end-time"} "End Time"]
     [:input {:id "end-time" :name "end-time" :type "datetime-local"}]

     [:label {:for "type"} "Type"]
     [:select {:id "type" :name "type"}
      [:option {:value "activity"} "activity"]]

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
      [:option {:value "hal5-long-table"} "Hal 5 - long table"]]

     [:label {:for "capacity"} "Capacity"]
     [:input {:id "capacity" :name "capacity" :type "number" :value 0}]

     [:label {:for "description"} "Description"]
     [:textarea {:id "description" :name "description"}]

     [:label {:for "ticket"} "Requires Ticket?"]
     [:input {:id "ticket" :name "ticket-required?" :type "checkbox"}]

     [:label {:for "published"} "Published/Visible?"]
     [:input {:id "published" :name "published?" :type "checkbox"}]

     [:input {:type "submit" :value "Create"}]]]))
