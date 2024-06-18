(ns co.gaiwan.compass.html.home
  {:ornament/prefix "home-"}
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.oauth :as oauth]
   [lambdaisland.ornament :as o]))

(o/defstyled nav-bar :nav
  t/flex
  t/surface-1
  {:align-items "center"
   :padding t/--size-3
   }
  [:h1 {:font-size   t/--font-size-5
        :margin-right "auto"
        :margin-left t/--size-2}]
  [:svg {:flex-grow   0
         :flex-shrink 0
         :width       t/--font-size-5
         :height      t/--font-size-5}]
  [graphics/cross {:display "none"}]
  ([user]
   [:<>
    [graphics/compass-logo]
    [:h1 "Compass"]
    [:button {:cx-toggle "menu-open" :cx-target "body"}
     [graphics/cross]
     [graphics/hamburger]]
    ]))

#_
(if-let [name (:user/name user)]
  name
  [:a {:href (oauth/flow-init-url)} "Sign-in with Discord"])

(o/defstyled menu-panel :nav
  t/surface-2
  t/h-full
  {:overflow "hidden"
   :animation "var(--animation-slide-in-left) forwards"
   :padding t/--size-3
   :box-shadow t/--shadow-4
   :position "absolute"
   :right 0
   :top 0
   :width 0}
  ([]
   "Menu"))

(o/defrules toggle-menu-button)

(o/defrules toggle-menu
  [:body.menu-open
   [menu-panel {:width t/--size-fluid-9}]
   [graphics/cross {:display "block"}]
   [graphics/hamburger {:display "none"}]])

(o/defined-garden)

(o/defstyled home :div
  [:main {:padding t/--size-3}]
  [:>div {:position "relative"}]
  ([user]
   [:<>
    [nav-bar user]
    [:div
     [:main
      "content"]
     [menu-panel]]
    ]))

