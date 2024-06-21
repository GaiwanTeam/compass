(ns co.gaiwan.compass.html.sessions
  {:ornament/prefix "sessions-"}
  (:require
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.oauth :as oauth]
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
   :mask          (str "linear-gradient(#0000 0 0) content-box intersect, conic-gradient(#000 " --arc-degrees ", #0000 0)") })

(o/defstyled capacity-gauge :div
  "Image with an arc around it to indicate how full a session is."
  :flex :aspect-square
  [arc :w-full]
  {--arc-thickness "10%"}
  [:.img :w-full
   {:padding --arc-thickness
    :margin-left "-100%"}
   [:>* :w-full :aspect-square :rounded-full]]
  ([{:keys [capacity image]}]
   [:<> {:style {--arc-degrees (str (* 360.0 capacity) "deg")}}
    [arc]
    [:div.img
     [:div
      {:style {:background-image image}}]]]))

(def test-values
  {:title ["Cursive Office Hours"
           "On Abstraction"
           "Let's go to the climbing gym"
           "Babashka Workshop"
           "The Curious Case of the Unexpected Unquote-Splice"
           "Bookswap"]
   :speaker ["Eric Normand" "Jack Rusher" "James Reeves" "Sophia Velten" "Michiel Borkent, Christian Johansen, Teodor Dorlunt" nil nil]
   :type [:talk :workshop :office-hours :workshop :activity]
   :organized ["Heart of Clojure" "Community" "Jordan Miller" "London Clojurians" "Dave Liepmann"]
   :day ["Wednesday" "Thursday"]
   :date ["17.09" "18.09"]
   :time (map #(str % ":" (rand-nth ["00" "15" "30"])) (range 9 20))
   :location ["Het Depot" "Hal 5" "HoCafé" "Hal 5 - Workshop Area" ]})

(defn rand-session []
  (update-vals test-values rand-nth))

(o/defstyled session-card :div
  :flex :gap-3
  :surface-2
  :shadow-4
  :px-3
  :py-2
  :boder :border-solid :border-surface-3
  [:.type {:background-color "light-dark(var(--red-2), var(--red-9))"}]
  [:&.talk [:.type {:background-color "light-dark(var(--blue-2), var(--blue-9))"}]]
  [:&.workshop [:.type {:background-color "light-dark(var(--teal-2), var(--teal-8))"}]]
  [:.title :mb-3 :font-size-3]
  [:.datetime :flex-col :items-center :justify-center :font-size-3 :font-bold]
  [:.guage :p-2]
  [:.type :font-bold
   :p-1
   :text-center
   :small-caps
   {:writing-mode "sideways-lr"}]
  [capacity-gauge :w-100px :mr-3]
  [:.details :flex-col :py-2]
  ([{:keys [type title speaker organized day date time location]}]
   [:<> {:class (name type)}
    [:div.type (name type)]
    [:div.datetime
     [:div (subs day 0 3)]
     [:div date]
     [:div time]]
    [:div.guage
     [capacity-gauge {:capacity (rand)
                      :image (str "var(--gradient-" (inc (rand-int 7)) ")")}]]
    [:div.details
     [:h2.title title]
     (when speaker
       [:p.speaker "Speaker " speaker])

     [:div.loc location]
     [:p.host "Organized by "organized]]]))
