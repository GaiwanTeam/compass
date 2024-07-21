(ns co.gaiwan.compass.html.navigation
  (:require
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.http.oauth :as oauth]
   [lambdaisland.ornament :as o]))

(o/defstyled nav-bar :nav
  :flex :items-center
  :bg-surface-1
  [:h1 :font-size-5 :mr-auto :ml-2]
  [:svg :grow-0 :shrink-0
   {:width  t/--font-size-5
    :height t/--font-size-5}]
  ([user]
   [:<>
    [graphics/compass-logo]
    [:h1 [:a {:href "/"} "Compass"]]
    [:button {:cx-toggle "menu-open" :cx-target "body"}
     [graphics/hamburger]]]))

(o/defstyled menu-panel :nav
  :bg-surface-2
  :h-screen
  :overflow-hidden :shadow-5 :z-1 :fixed
  {:transition "transform 300ms ease-in"
   :width      t/--size-fluid-10
   :right      0
   :max-width  "100vw"
   :transform  "translate(100%, 0)"}
  [:svg {:width  t/--font-size-5
         :height t/--font-size-5}]
  [:.bar :flex :justify-between :p-3]
  [:li :font-size-3
   :line-height-5 ;; FIXME
   :border :border-solid :border-surface-4
   {:font-size     t/--font-size-3}]
  ([user]
   [:<>
    [:div.bar
     "Menu"
     [:button {:cx-toggle "menu-open" :cx-target "body"}
      [graphics/cross]]]
    #_[:pre (pr-str user)]
    [:ul

     [:li
      (if-let [name (:user/name user)]
        [:<>
         [:p "Welcome, " name]
         [:a {:href "/logout"} "Sign out"]]
        [:a {:href (oauth/flow-init-url)} "Sign-in with Discord"])]
     [:li [:a {:href "/"} "Sessions & Activities"]]
     [:li [:a {:href "/"} "Attendees"]]
     [:li [:a {:href "/"} "Profile & Settings"]]
     [:li [:a {:href "/sessions/new"} "Create Activity"]]]]))

(o/defrules toggle-menu-button)

(o/defrules toggle-menu
  [:body.menu-open
   [menu-panel {:transform "translate(0, 0)"}]])
