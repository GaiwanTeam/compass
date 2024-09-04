(ns co.gaiwan.compass.html.sessions
  "Views and components (hiccup/ornament) related to sessions"
  {:ornament/prefix "sessions-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.html.components :as c]
   [co.gaiwan.compass.html.filters :as filters]
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.session :as session]
   [co.gaiwan.compass.model.user :as user]
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
   {:font-size "5rem"}]
  [:&.checked
   [:.checkmark :flex]
   [:.img {:filter "brightness(50%)"}]]
  [:.img :w-full
   {:padding --arc-thickness
    #_#_:margin-left "-100%"}
   [:>* :w-full :aspect-square :rounded-full
    {:background-size "cover"
     :background-position "50% 50%"}]]
  [graphics/checkmark :p-3 {t/--icon-color t/--hoc-green}]
  ([{:keys [capacity image checked?]}]
   [:<> {:class [(when checked? "checked")]
         :style {--arc-degrees (str (* 360.0 capacity) "deg")}}
    [arc {:style {--arc-degrees "360deg"
                  --arc-color "white"}}]
    [arc {:style {--arc-color (if (< capacity 0.5)
                                t/--hoc-green
                                t/--hoc-pink-4)
                  :filter (str "brightness(" (if (< capacity 0.5)
                                               (* 110 (+ 1 (- 0.5 capacity)))
                                               (* 110 (- 1.5 (- capacity 0.5)))) "%)")}}]
    [:div.img
     [:div
      {:style {:background-image image}}]]
    [:div.checkmark [:div [graphics/checkmark]]]]))

(declare session-card)

(o/defstyled join-btn c/form
  [:input {:color t/--text-2
           :background-color t/--surface-3
           :border-radius t/--radius-2}]
  ([session user]
   ;; Progressive enhancement, without htmx the form submission will kick in
   [:<>
    {:method "POST"
     :action (url-for :session/participate {:id (:db/id session)})}
    [:input {:type "submit"
             :hx-post (url-for :session/participate {:id (:db/id session)})
             :hx-swap "none"
             :value
             (if (session/participating? session user)
               "Leave"
               "Join")}]]))

(o/defstyled session-card-actions :nav
  :flex :justify-end :w-full
  :mt-2
  ([session user]
   [:<>
    [join-btn session user]
    [:a.btn {:href (url-for :session/get {:id (:db/id session)})}
     "Details"]]))

(o/defprop --session-type-color)

(o/defstyled session-image+guage :div
  :p-2
  [capacity-gauge :w-100px]
  ([{:session/keys [signup-count capacity] :as session} user]
   [capacity-gauge {:capacity #_(rand) (/ (or signup-count 0) (max capacity 1))
                    :image (session/session-image-css-value session)
                    :checked? (session/participating? session user)}]))

(defn fmt-dur [dur-str]
  (let [d (time/duration dur-str)
        h (.toHours d)
        m (.toMinutesPart d)]
    (str
     (when (< 0 h)
       (str h " hrs "))
     (when (< 0 m)
       (str m " min")))))

(o/defstyled session-card :div
  :flex :gap-1
  :bg-surface-2
  :shadow-2
  :boder :border-solid :border-surface-3
  #_:text-center
  [:.left :flex-col :items-center :py-3 :mx-2]
  [:.title :font-size-4 :font-semibold :mt-3 :mb-2
   [:a {:color t/--text-1}]]
  [:.subtitle :font-size-3 :font-medium :mb-3
   {:color t/--text-2}]
  [:.details :w-full #_:items-center :py-3 :mr-2 :relative]
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
  [:&.htmx-request
   [#{:.left :.details}
    {:opacity "0.5"
     :animation "session-card-pulse 1s cubic-bezier(0.4, 0, 0.6, 1) infinite"}]]
  ([{:session/keys [type title subtitle organized time
                    location image participants duration
                    capacity signup-count] :as session}
    user]
   [:<>
    {:hx-get (url-for :session/card {:id (:db/id session)})
     :hx-trigger (str "session-" (:db/id session) "-updated from:body")
     :hx-target (str "closest ." session-card)
     :hx-select (str "." session-card " > *")
     :style {--session-type-color (:session.type/color type)}
     ;; :cx-toggle "expanded"
     ;; :cx-target (str "." session-card)
     :hx-disinherit "hx-target hx-select"}
    [:div.type (:session.type/name type)]

    [:div.left
     [session-image+guage session user]
     [join-btn session user]]

    [:div.details
     [:h2.title
      [:a {:href (url-for :session/get {:id (:db/id session)})}
       [:span.datetime
        (str (time/truncate-to (time/local-time time) :minutes)) " · "]
       title]]
     [:h3.subtitle subtitle]
     #_[:div.expansion
        [session-card-actions session user]]
     [:div.loc (fmt-dur duration) " @ " (:location/name location)]
     #_[:p.host "Organized by " organized]]]))

(o/defrules session-card-pulse
  (garden.stylesheet/at-keyframes
   :session-card-pulse
   ["0%, 100%" {:opacity 1}]
   ["50%" {:opacity 0.5}]))

(o/defstyled attendee :li
  ([participant]
   ;; (prn "debug datatype " (type participant))
   ;; participant is of `Datomic.query.EntityMap` type
   ;; So, we can access its attribute directly
   (:user/handle participant)))

(o/defstyled session-detail :div
  [capacity-gauge :w-100px]
  :mt-8
  [:.header-row :flex :gap-2 :mb-8
   [:.title :font-size-8 {:text-wrap :wrap}]
   [:.header-row-text]
   [:.type
    {:background --session-type-color}
    :m-0 :font-bold :uppercase :tracking-widest :p-1]]
  [:.event-at
   {:box-shadow "-14px 14px 0 -4px black"
    :background t/--highlight-yellow}
   :p-4 :max-w-lg :mt-2 :mb-6 :ml-2
   [:>p :font-semibold]
   [:.datetime :font-size-7 :font-bold]]
  [:.three-box #_{:background t/--activity-color}
   :relative
   :font-size-6 :my-4 :flex :p-4 :gap-4
   [:>div :border-8 :font-semibold :p-4 :w-33% :text-center
    [:>.small :font-size-3 :uppercase :tracking-widest]
    [:>.large :font-size-7 :font-bold]]
   [:&:before
    {:content "''"
     :position "absolute"
     :top "0"
     :left "5%"
     :width "90%"
     :height "110%"
     :z-index -1
     :background-color t/--activity-color
     :border-radius "900px"
     :transform "rotate(1deg)"}]]

  ([{:session/keys [type title subtitle organized
                    time location image capacity
                    signup-count description
                    participants] :as session}
    user]
   [:<>
    {:hx-get (url-for :session/get {:id (:db/id session)})
     :hx-trigger (str "session-" (:db/id session) "-updated from:body")
     :hx-target (str "closest ." session-detail)
     :hx-select (str "." session-detail " > *")
     :hx-disinherit "hx-target hx-select "
     :style {--session-type-color (:session.type/color type)}}

    [:div.details
     [:a
      {:href (url-for :sessions/index)
       :style {:display "none"}
       :hx-trigger (str "session-" (:db/id session) "-deleted from:body")}]
     [:div.header-row
      [session-image+guage session user]
      [:div.header-row-text
       [:div.type (:session.type/name type)]
       [:h3.title title]]]
     [:div.event-at
      [:p "Event scheduled at"]
      [:div.datetime
       (str (time/truncate-to (time/local-time time) :minutes)
            ", "
            (subs (str/capitalize (str (time/day-of-week time))) 0 3)
            " "
            (time/format "dd.MM" time))]]
     [:h3.subtitle subtitle]
     [:div.description
      [:div (m/component (m/md->hiccup description))]]
     [:div.three-box
      [:div.location
       [:div.small "Location "]
       [:div.large (:location/name location)]]
      [:div.capacity
       [:div.small "Spots available"]
       [:div.large (- (or capacity 0) (or signup-count 0))]]
      [:div
       [:p.small "Ticket required"]
       (if (:session/ticket-required? session)
         [:p.large "YES ✅"]
         [:p.large "NO ❎"])]]
     (when (session/organizing? session user)
       ;; Only show the participants' list to organizer.
       [:div.participants
        [:div "Participants:"]
        [:ol (map attendee participants)]])

     [:div.actions
      [join-btn session user]
      (when (or (user/admin? user)
                (session/organizing? session user))
        [:<>
         [:a {:href (url-for :session/edit {:id (:db/id session)})}
          [:button  "Edit"]]
         [:button {:hx-delete (url-for :session/get {:id (:db/id session)})} "Delete"]])]
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
  [:>h2 :mb-4 :mt-7
   {:font-size t/--font-size-3}
   ["&:first-child" :mt-0]
   [:at-media {:min-width "24rem"} {:font-size t/--font-size-4}]
   [:at-media {:min-width "40rem"} {:font-size t/--font-size-5}]]
  ([{:keys [user sessions]}]
   [:<>
    {:hx-get     (url-for :sessions/index)
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

(def xxx #time/zdt "2024-08-16T10:30+02:00[Europe/Brussels]")

(o/defstyled session-form :div
  [#{:label :input} :block]
  [:label
   :mb-1 :mt-2
   {:font-size t/--font-size-3
    :font-weight t/--font-weight-6}]
  [#{:input :textarea :select} ["&:not([type=checkbox])" :w-full :mb-3]]
  [:label
   :justify-start
   :items-center
   ["&:has([type=checkbox])"
    :flex
    :gap-3]]
  [:div.date-time :flex :gap-2]
  ([user session]
   [:<>
    (if session
      [:h2 "Edit Activity"]
      [:h2 "Create Activity"])
    [:form
     (-> (if session
           {:hx-patch (url-for :session/get {:id (:db/id session)})}
           {:method "POST" :action (url-for :session/save)})
         (assoc :enctype "multipart/form-data"))
     [:input {:type "hidden" :name "organizer-id" :value (:db/id user)}]
     [:label {:for "title"} "Name of Your Activity"]
     [:input (cond-> {:id "title" :name "title" :type "text"
                      :required true :min-length 2}
               session
               (assoc :value (:session/title session)))]
     [:label {:for "subtitle"} "Subtitle (optional)"]
     [:input (cond-> {:id "subtitle" :name "subtitle" :type "text"
                      :min-length 10}
               session
               (assoc :value (:session/subtitle session)))]
     [:label {:for "start-time"} "Day and Start Time"]
     [:div.date-time
      [:input {:id "start-date" :name "start-date" :type "date"
               :value (if session
                        (str (time/local-date (:session/time session)))
                        (str (java.time.LocalDate/now)))}]
      [:input (cond->
               {:id "start-time" :name "start-time" :type "time"
                :min "06:00" :max "23:00" :required true
                :step 60}
                session
                (assoc :value
                       (str (time/local-time (:session/time session)))))]]
     [:label {:for "duration-time"} "Duration in minutes"]
     [:input
      {:id "duration-time" :name "duration-time"
       :type "number"
       :value (if session
                (session/duration (:session/duration session))
                45)}]

     [:label {:for "type"} "Type"]
     [:select {:id "type" :name "type"}
      [:option {:value "activity"} "Activity"]]

     [:label {:for "location"} "Location"]
     [:select (cond-> {:id "location" :name "location"}
                session
                (assoc :value
                       (name (get-in session [:session/location :db/ident]))))
      [:option {:value "depot-main-stage"} "Het Depot - main stage"]
      [:option {:value "depot-bar"} "Het Depot - Bar"]
      [:option {:value "hal5-zone-a"} "Hal 5 - zone A"]
      [:option {:value "hal5-zone-b"} "Hal 5 - zone B"]
      [:option {:value "hal5-hoc-cafe"} "Hal 5 - HoC Café"]
      [:option {:value "hal5-foodcourt"} "Hal 5 - Foodcourt"]
      [:option {:value "hal5-park"} "Hal 5 - park"]
      [:option {:value "hal5-outside-seating"} "Hal 5 - outside seating"]
      [:option {:value "hal5-long-table"} "Hal 5 - long table"]]

     [:label {:for "capacity"} "How many people can you accomodate?"]
     [:input (cond-> {:id "capacity" :name "capacity" :type "number"
                      :min 2 :value 5 :required true}
               session
               (assoc :value (:session/capacity session)))]

     [:label {:for "description"} "Description (supports Markdown)"]
     [:textarea {:id "description" :name "description"}
      (when session
        (:session/description session))]

     [:label {:for "ticket"}
      [:input {:id "ticket" :name "ticket-required?" :type "checkbox"
               :checked (:session/ticket-required? session)}]
      "Requires Ticket?"]

     [:label {:for "published"}
      [:input {:id "published" :name "published?" :type "checkbox"
               :checked (:session/published? session)}]
      "Published/Visible?"]

     (when session
       [session-image+guage session user])
     [:label {:for "image"} "Activity Image"]
     [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]

     [:input {:type "submit" :value (if session
                                      "Save"
                                      "Create")}]]]))

(o/defstyled session-list+filters :div
  ([{:keys [user sessions filters]}]
   [:<>
    [filters/filter-section filters]
    [session-list {:user user
                   :sessions sessions}]]))
