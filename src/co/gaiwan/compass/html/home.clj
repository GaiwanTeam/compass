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
  {:padding     t/--size-3
   :align-items "center"}
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
    [graphics/cross {::o/attrs {:class "btn"}}]
    ]))
#_
(if-let [name (:user/name user)]
  name
  [:a {:href (oauth/flow-init-url)} "Sign-in with Discord"])

(o/defstyled home :main
  ([user]
   [:<>
    [nav-bar user]
    ]))

