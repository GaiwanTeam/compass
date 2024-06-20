(ns co.gaiwan.compass.html.home
  {:ornament/prefix "home-"}
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.html.sessions :as sessions]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.oauth :as oauth]
   [lambdaisland.ornament :as o]))

(o/defstyled nav-bar :nav
  t/flex
  t/surface-1
  {:align-items "center"
   :padding t/--size-3}
  [:h1 {:font-size   t/--font-size-5
        :margin-right "auto"
        :margin-left t/--size-2}]
  [:svg {:flex-grow   0
         :flex-shrink 0
         :width       t/--font-size-5
         :height      t/--font-size-5}]
  ([user]
   [:<>
    [graphics/compass-logo]
    [:h1 "Compass"]
    [:button {:cx-toggle "menu-open" :cx-target "body"}
     [graphics/hamburger]]
    ]))

#_
(if-let [name (:user/name user)]
  name
  [:a {:href (oauth/flow-init-url)} "Sign-in with Discord"])

(o/defstyled menu-panel :nav
  t/surface-2
  t/h-full
  [:svg {:width       t/--font-size-5
         :height      t/--font-size-5}]
  [:.bar :flex :justify-between
   {:padding t/--size-3}]
  [:li {:font-size t/--font-size-3
        :line-height t/--font-size-6
        :border-bottom (str "1px solid " t/--surface-4)}]
  {:overflow "hidden"
   :transition "transform 300ms ease-in"
   :box-shadow t/--shadow-5
   :position "fixed"
   :width t/--size-fluid-10
   :right 0
   :max-width "100vw"
   :transform "translate(100%, 0)"
   :z-index 1}
  ([]
   [:<>
    [:div.bar
     "Menu"
     [:button {:cx-toggle "menu-open" :cx-target "body"}
      [graphics/cross]]]
    [:ul
     [:li [:a {:href "/"} "Sessions & Activities"]]
     [:li [:a {:href "/"} "Attendees"]]
     [:li [:a {:href "/"} "Profile & Settings"]]
     [:li [:a {:href "/"} "Create Activity"]]]]))

(o/defrules toggle-menu-button)

(o/defrules toggle-menu
  [:body.menu-open
   [menu-panel {:transform "translate(0, 0)"}]
   ])


(o/defstyled home :div
  [:main {:padding t/--size-3
          }
   ]
  {:max-width "100vw"}
  :overflow-hidden
  [sessions/session-card :mb-3]
  ([user]
   [:<>
    [menu-panel]
    [nav-bar user]
    [:main
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]]
    ]))

