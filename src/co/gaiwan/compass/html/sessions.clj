(ns co.gaiwan.compass.html.sessions
  "Views and components (hiccup/ornament) related to sessions"
  {:ornament/prefix "sessions-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.db.queries :as q]
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
(o/defprop --arc-color --lime-5)

(o/defstyled arc :div
  "Partial circle arc, clockwise. Expects arc (a value in CSS `deg` units) and
  thickness to be passed as props or set as css vars by a parent element."
  {:aspect-ratio  --ratio-square
   :padding       t/--arc-thickness
   :border-radius --radius-round
   :background    --arc-color
   :mask          (str "linear-gradient(#0000 0 0) content-box intersect, conic-gradient(#000 " --arc-degrees ", #0000 0)")})

(o/defstyled capacity-gauge :div
  "Image with an arc around it to indicate how full a session is."
  :aspect-square
  {:position "relative"}
  [:>* {:position "absolute" :top 0 :left 0}]
  [arc :w-full]
  {t/--arc-thickness "7%"}
  [:.checkmark :hidden :w-full :justify-center :h-full :items-center
   {:font-size "5rem"}]
  [:&.checked
   [:.checkmark :flex]
   [:.img {:filter "brightness(50%)"}]]
  [:.img :w-full
   {:padding t/--arc-thickness
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
  [:span :font-bold]
  ([session user]
   ;; Progressive enhancement, without htmx the form submission will kick in
   (if (<= (:session/capacity session) (:session/signup-count session))
     [:<>
      [:span "FULL"]]
     [:<>
      {:method "POST"
       :action (url-for :session/participate {:id (:db/id session)})}
      [:input {:type "submit"
               :hx-post (url-for :session/participate {:id (:db/id session)})
               :hx-swap "none"
               :value
               (if (session/participating? session user)
                 "Leave"
                 "Join")}]])))

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

(o/defstyled img+join-widget :div
  :flex-col :items-center :py-3 :mx-2
  ([session user]
   [:<>
    [session-image+guage session user]
    [join-btn session user]]))

(o/defstyled session-card :div
  :flex :gap-1
  :bg-surface-2
  :shadow-2
  :boder :border-solid :border-surface-3
  #_:text-center
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
   [#{(str "." img+join-widget) :.details}
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

    [img+join-widget session user]

    [:div.details
     [:h2.title
      [:a {:href (url-for :session/get {:id (:db/id session)})}
       [:span.datetime
        (str (time/truncate-to (time/local-time time) :minutes)) " · "]
       title]]
     [:h3.subtitle (session/subtitle session)]
     #_[:div.expansion
        [session-card-actions session user]]
     [:div.loc (fmt-dur duration) " @ " (:location/name location)]
     #_[:p.host "Organized by " organized]]]))

(o/defrules session-card-pulse
  (garden.stylesheet/at-keyframes
   :session-card-pulse
   ["0%, 100%" {:opacity 1}]
   ["50%" {:opacity 0.5}]))

(o/defstyled handle-the-hidden :div
  ([participants]
   (let [hiddens (count (filter #(:public-profile/hidden? %) participants))]
     (when (pos? hiddens)
       (str "and " hiddens " more")))))

(o/defstyled attendee :div
  :flex :items-center :my-2 :py-2
  :shadow-2 :font-size-3
  {:background-color t/--surface-2}
  [:.details :flex-grow :mr-2]
  [c/image-frame :w-50px {--arc-thickness "7%"} :mx-2]
  ([p]
   ;; (prn "debug datatype " (type participant))
   ;; participant is of `Datomic.query.EntityMap` type
   ;; So, we can access its attribute directly
   [:<>
    [c/image-frame {:profile/image (user/avatar-css-value p)}]
    [:div.details
     [:a {:href (url-for :profile/show {:user-uuid (:user/uuid p)})}
      [:div.profile-name (:public-profile/name p)]]]]))

(o/defstyled session-detail :div
  [capacity-gauge :w-100px]
  :mt-8
  [:.header-row :flex :gap-2 :mb-8
   :items-center
   [:.title :lg:font-size-8
    :font-size-7
    {:text-wrap :wrap
     :word-break :break-word}]
   [:.header-row-text]
   [:.type
    {:background --session-type-color}
    :my-1 :font-bold :uppercase :tracking-widest :p-1]]
  [:.event-at
   {:box-shadow "-14px 14px 0 -4px black"
    :background t/--highlight-yellow
    :max-width "34rem"}
   :p-4  :mt-2 :mb-6 :ml-2
   [:>p :font-semibold]
   [:.datetime :font-size-7 :font-bold]]
  [:.three-box #_{:background t/--activity-color}
   :relative :my-4 :p-4 :gap-4 :lg:flex-row :flex-col
   [:>div :border-8 :font-semibold :p-4 :text-center :flex-grow
    [:>.small :lg:font-size-3 :uppercase :tracking-widest]
    [:>.large :font-size-6 :lg:font-size-7 :font-bold]]
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
                    time duration location image capacity
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
      [img+join-widget session user]
      [:div.header-row-text
       [:div [:span.type (:session.type/name type)]]
       [:h3.title title]
       [:h4 (session/subtitle session)]]]
     [:div.event-at
      #_[:p "Event scheduled at"]
      [:div.datetime
       (when time
         (str
          (subs (str/capitalize (str (time/day-of-week time))) 0 3)
          " "
          (time/format "dd.MM" time)
          ", "
          (time/truncate-to (time/local-time time) :minutes)))
       " → "
       (fmt-dur duration)]]

     [:div.description.site-copy
      [:div (m/component (m/md->hiccup description))]]
     [:div.three-box
      [:div.location
       [:div.small "Location "]
       [:div.large (:location/name location)]]
      [:div.capacity
       [:div.small "Spots available"]
       [:div.large (min 0 (- (or capacity 0) (or signup-count 0)))]]
      #_[:div
         [:p.small "Ticket required"]
         (if (:session/ticket-required? session)
           [:p.large "YES ✅"]
           [:p.large "NO ❎"])]]
     #_(when (session/organizing? session user))
     [:div.participants
      [:h3 "Participants (" signup-count ")"]
      (for [p participants]
        (when-not (:public-profile/hidden? p)
          [attendee p]))
      [handle-the-hidden participants]]

     [:div.actions

      (when (or (user/admin? user)
                (session/organizing? session user))
        [:<>
         [:button
          {:hx-post (url-for :session/create-thread {:id (:db/id session)})
           :title "Create a Discord thread for participants of this session"
           :disabled (some? (:session/thread-id session))}
          "Create Thread"]
         [:br]
         [:a {:href (url-for :session/edit {:id (:db/id session)})}
          [:button  "Edit"]]
         [:button {:hx-delete (url-for :session/get {:id (:db/id session)})} "Delete"]])]
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
  ([user session session-types]
   (def session session)
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
     (when (user/admin? user)
       [:<>
        [:label {:for "subtitle"} "Subtitle (optional)"]
        [:input (cond-> {:id "subtitle" :name "subtitle" :type "text"
                         :min-length 10}
                  session
                  (assoc :value (:session/subtitle session)))]])
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

     (when (user/admin? user)
       [:<>
        [:label {:for "type"} "Type"]
        [:select {:id "type" :name "type"}
         (for [{n :session.type/name id :db/id} session-types]
           [:option (cond-> {:value id}
                      (= id (:db/id (:session/type session)))
                      (assoc :selected "selected"))
            n])]])

     [:label {:for "location"} "Location"]

     [:select {:id "location" :name "location"}
      (for [{:location/keys [name] :db/keys [id]} (q/all-locations)]
        [:option (cond-> {:value id}
                   (= id (get-in session [:session/location :db/id]))
                   (assoc :selected "selected"))
         name])]

     [:label {:for "capacity"} "How many people can you accomodate?"]
     [:input (cond-> {:id "capacity" :name "capacity" :type "number"
                      :min 2 :value 5 :required true}
               session
               (assoc :value (:session/capacity session)))]

     [:label {:for "description"} "Description (supports Markdown)"]
     [:textarea {:id "description" :name "description"}
      (when session
        (:session/description session))]

     #_[:label {:for "ticket"}
        [:input {:id "ticket" :name "ticket-required?" :type "checkbox"
                 :checked (:session/ticket-required? session)}]
        "Requires Ticket?"]

     #_[:label {:for "published"}
        [:input {:id "published" :name "published?" :type "checkbox"
                 :checked (:session/published? session)}]
        "Published/Visible?"]

     (when session
       [session-image+guage session user])
     [:label {:for "image"} "Activity Image"]
     [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg, image/gif, image/webp, image/svg+xml"}]

     [:input {:type "submit" :value (if session
                                      "Save"
                                      "Create")}]]]))

(o/defstyled session-list+filters :div
  ([{:keys [user sessions filters]}]
   [:<>
    [filters/filter-section filters]
    [session-list {:user user
                   :sessions sessions}]]))
