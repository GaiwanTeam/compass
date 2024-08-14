(ns co.gaiwan.compass.html.sessions
  "Views and components (hiccup/ornament) related to sessions"
  {:ornament/prefix "sessions-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.html.filters :as filters]
   [co.gaiwan.compass.model.session :as session]
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

(o/defstyled participate-btn :button
  {:--_bg t/--surface-3
   :--_border "none"
   :--_text t/--text-2}
  ([session user]
   [:<> (if user
          {:hx-post (str "/sessions/" (:db/id session) "/participate")
           :hx-indicator (str ".c" (:db/id session))
           :hx-swap "none"}
          {:hx-target "#modal"
           :hx-get "/login"})
    (if (session/participating? session user)
      "Leave"
      "Join")]))

(o/defstyled session-card-actions :nav
  :flex :justify-end :w-full
  :mt-2
  ([session user]
   [:<>
    [participate-btn session user]
    [:a.btn {:href (str "/sessions/" (:db/id session))}
     "Details"]]))

(o/defprop --session-type-color)

(o/defstyled session-image+guage :div
  :p-2
  [capacity-gauge :w-100px]
  ([{:session/keys [signup-count capacity image] :as session} user]
   [capacity-gauge {:capacity (/ (or signup-count 0) (max capacity 1))
                    :image (if image
                             (str "url(" image ")")
                             (str "var(--gradient-" (inc (rand-int 7)) ")"))
                    :checked? (session/participating? session user)}]))

(o/defstyled session-card :div
  :flex :gap-1
  :bg-surface-2
  :shadow-2
  :boder :border-solid :border-surface-3
  #_:text-center
  [:.left :flex-col :items-center {:float "left"} :mx-2]
  [:.title :font-size-4 :font-semibold :mt-3 :mb-2]
  [:.subtitle :font-size-3 :font-medium :mb-3
   {:color t/--text-2}]
  [:.details :w-full #_:items-center :py-3 :relative]
  [:.type :font-bold
   :p-1
   :text-center
   :small-caps
   {:writing-mode "vertical-lr"
    :transform "rotate(180deg)"
    :background-color --session-type-color}]
  [:.loc {:color t/--text-2}]

  [session-card-actions :text-right]
  [:.expansion {:display "none"}]
  [:&.expanded [:.expansion {:display "block"}]]
  ([{:session/keys [type title subtitle organized time
                    location image participants
                    capacity signup-count] :as session}
    user]
   [:<>
    {:hx-get (str "/sessions/" (:db/id session) "/card")
     :hx-trigger (str "session-" (:db/id session) "-updated from:body")
     :hx-target (str "closest ." session-card)
     :hx-select (str "." session-card " > *")
     :style {--session-type-color (:session.type/color type)}
     ;; :cx-toggle "expanded"
     ;; :cx-target (str "." session-card)
     :hx-disinherit "hx-target hx-select"}
    [:div.type (:session.type/name type)]

    [:div.details
     {:class ["session-card-pulse" (str "c" (:db/id session))]}
     [:div.left
      [session-image+guage session user]
      [participate-btn session user]]

     [:h2.title
      [:span.datetime
       (str (time/truncate-to (time/local-time time) :minutes)) " · "]
      title]
     [:h3.subtitle subtitle]
     #_[:div.expansion
        [session-card-actions session user]]
     [:div.loc "@ " (:location/name location)]
     #_[:p.host "Organized by " organized]]]))

(o/defstyled attendee :li
  ([participant]
   ;; (prn "debug datatype " (type participant))
   ;; participant is of `Datomic.query.EntityMap` type
   ;; So, we can access its attribute directly
   (:user/handle participant)))

(o/defstyled session-detail :div
  [capacity-gauge :w-100px]
  ([{:session/keys [type title subtitle organized
                    time location image capacity
                    signup-count description
                    participants] :as session}
    user]
   [:<>
    {:hx-get (str "/sessions/" (:db/id session))
     :hx-trigger (str "session-" (:db/id session) "-updated from:body")
     :hx-target (str "closest ." session-detail)
     :hx-select (str "." session-detail " > *")
     :hx-disinherit "hx-target hx-select "
     :style {--session-type-color (:session.type/color type)}}
    [:div.type (:session.type/name type)]

    [:div.details
     [session-image+guage session user]
     [:h3.title title]
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
      [:div "Spots available:"]
      [:div (- (or capacity 0) (or signup-count 0))]]
     (when (session/organizing? organized user)
       ;; Only show the participants' list to organizer.
       [:div.participants
        [:div "Participants:"]
        [:ol (map attendee participants)]])
     (when (:session/ticket-required? session)
       [:p "Required Ticket"])
     [:div.actions
      [participate-btn session user]
      (when (session/organizing? organized user)
        ;; Only allow the event organizer to edit this event
        [:button "Edit"])]
     #_[:p.host "Organized by " organized]
     #_[:ol (map attendee participants)]
     #_[:p (pr-str user)]
     #_[:p (pr-str session)]]]))

(o/defstyled session-list :section#sessions
  [:.sessions
   :grid :gap-3
   {:grid-template-columns "repeat(1, 1fr)"}
   [:at-media {:min-width "40rem"} {:grid-template-columns "repeat(1, 1fr)"}]
   [:at-media {:min-width "60rem"} {:grid-template-columns "repeat(2, 1fr)"}]
   #_[:at-media {:min-width "80rem"} {:grid-template-columns "repeat(3, 1fr)"}]]
  [:>h2 :mb-4 :mt-8
   {:font-size t/--font-size-3}
   [:at-media {:min-width "24rem"} {:font-size t/--font-size-4}]
   [:at-media {:min-width "40rem"} {:font-size t/--font-size-5}]]
  ([{:keys [user sessions]}]
   [:<>
    {:hx-get     "/sessions"
     :hx-trigger "filters-updated from:body"
     :hx-swap    "outerHTML"
     :hx-select  "#sessions"
     :hx-disinherit "hx-swap"}
    (for [[day sessions] (group-by #(time/truncate-to (:session/time %) :days) sessions)]
      [:<>
       [:h2 (time/format "EEEE, dd'th of' LLLL" day)]
       [:div.sessions
        (for [session (sort-by :session/time sessions)]
          [session-card session user])]])]))

;; Create / edit

(o/defstyled session-form :div
  [:form :grid {:grid-template-columns "10rem 1fr"} :gap-2]
  [:div.date-time {:display "flex" :gap "0.5rem"}]
  ([params]
   [:<>
    [:h2 "Create Activity"]
    [:form {:method "POST" :action "/sessions"
            :enctype "multipart/form-data"}
     [:label {:for "title"} "Title"]
     [:input {:id "title" :name "title" :type "text"
              :required true :min-length 2}]

     [:label {:for "subtitle"} "Subtitle"]
     [:input {:id "subtitle" :name "subtitle" :type "text"
              :min-length 10}]

     [:label {:for "start-time"} "Start Time"]
     [:div.date-time
      [:select {:id "start-date" :name "start-date"}
       (let [day-before 3
             day-after 3]
         (for [day (range (- 4 day-before) (+ 4 day-after))]
           [:option {:value (format "2024-08-%02d" day)} (format "2024-08-%02d" day)]))]
      [:input {:id "start-time" :name "start-time" :type "time"
               :min "06:00" :max "23:00" :required true
               :step (* 5 60)}]]

     [:label {:for "duration-time"} "Duration Time"]
     [:input.html-duration-picker
      {:id "duration-time" :name "duration-time" :data-hide-seconds true}]

     [:label {:for "type"} "Type"]
     [:select {:id "type" :name "type"}
      [:option {:value "activity"} "Activity"]]

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
     [:input {:id "capacity" :name "capacity" :type "number"
              :min 2 :value 2 :required true}]

     [:label {:for "description"} "Description"]
     [:textarea {:id "description" :name "description"
                 :required true}]

     [:label {:for "ticket"} "Requires Ticket?"]
     [:input {:id "ticket" :name "ticket-required?" :type "checkbox"}]

     [:label {:for "published"} "Published/Visible?"]
     [:input {:id "published" :name "published?" :type "checkbox"}]

     [:label {:for "image"} "Activity Image"]
     [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]

     [:input {:type "submit" :value "Create"}]]]))

(o/defstyled session-list+filters :div
  ([{:keys [user sessions filters]}]
   [:<>
    [filters/filter-section filters]
    [session-list {:user user
                   :sessions sessions}]]))
