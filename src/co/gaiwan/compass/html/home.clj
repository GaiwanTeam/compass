(ns co.gaiwan.compass.html.home
  {:ornament/prefix "home-"}
  (:require
   [lambdaisland.ornament :as o]
   [co.gaiwan.compass.css.tokens :as t :refer :all]))

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
  flex square
  [arc w-full]
  {--arc-thickness "10%"}
  [:.img w-full
   {:padding --arc-thickness
    :margin-left "-100%"}
   [:>* w-full square rounded]]
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
   :speaker ["Eric Normand" "Jack Rusher" "James Reeves" "Francine Bennett" "Sophia Velten" "Michiel Borkent, Christian Johansen, Teodor Dorlunt" nil nil]
   :type [:talk :workshop :office-hours :workshop :activity]
   :organized ["Heart of Clojure" "Community" "Jordan Miller" "London Clojurians" "Dave Liepmann"]
   :day ["Wednesday" "Thursday"]
   :date ["17.09" "18.09"]
   :time (map #(str % ":" (rand-nth ["00" "15" "30"])) (range 9 20))
   :location ["Het Depot" "Hal 5" "HoCafé" "Hal 5 - Workshop Area" ]})

(defn rand-session []
  (update-vals test-values rand-nth))

(o/defstyled session-card :div
  surface-2
  :shadow-4
  :p-tb-2
  :p-lr-3
  {:border (str "1px solid " --surface-3)}
  [:&.talk {:background-color "light-dark(var(--blue-2), var(--blue-9))"}]
  [:&.workshop {:background-color "light-dark(var(--teal-2), var(--teal-8))"}]
  [:.title
   :m-b-3
   {:font-size --font-size-3
    :max-inline-size "none"}]
  [:.content  flex]
  [capacity-gauge :w-100px :m-r-3]
  [:.details flex-col]
  ([{:keys [type title speaker organized day date time location]}]
   [:<> {:class (name type)}
    [:h2.title title]
    [:div.content
     [capacity-gauge {:capacity (rand)
                      :image (str "var(--gradient-" (inc (rand-int 7)) ")")}]
     [:div.details
      (when speaker
        [:p.speaker "Speaker " speaker])

      [:div.time day " " date " " time]
      [:div.loc location]
      [:p.host "Organized by "organized]]]]))

(o/defstyled home :main
  :p-3
  flex-col
  {:gap "var(--size-3)"}
  ([]
   [:<>
    (repeatedly 1 #(do [session-card (rand-session)]))]))
