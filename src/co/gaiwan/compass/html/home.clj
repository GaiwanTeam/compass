(ns co.gaiwan.compass.html.home
  (:require
   [lambdaisland.ornament :as o]))

(o/defstyled card :div
  :shadow-3
  :margin-2
  :padding-3
  :surface-1
  )

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
  :surface-2
  :shadow-3
  :padding-tb-2
  :padding-lr-3
  [:.title
   :margin-b-3
   {:font-size "var(--font-size-3)"
    :max-inline-size "none"}]
  [:.content :flex]
  [:.details :flex-col]
  [:.avatar
   :margin-r-3

   {:flex-shrink 0
    :width "var(--size-fluid-6)",
    :height "var(--size-fluid-6)",
    :border-radius "100%"}]
  ([{:keys [type title speaker organized day date time location]}]
   [:<>
    [:h2.title title]
    [:div.content
     [:div.avatar
      {:style {:background-image (str "var(--gradient-" (inc (rand-int 8)) ")")}}]
     [:div.details
      (when speaker
        [:p.speaker "Speaker " speaker])

      [:div.time day " " date " " time]
      [:div.loc location]
      [:p.host "Organized by "organized]]]]))

(o/defstyled home :main
  :padding-3
  :flex-col
  {:gap "var(--size-3)"}
  ([]
   [:<>
    (repeatedly 10 #(do [session-card (rand-session)]))]))
