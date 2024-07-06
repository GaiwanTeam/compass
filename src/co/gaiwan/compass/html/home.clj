(ns co.gaiwan.compass.html.home
  "The UI of the compass system"
  {:ornament/prefix "home-"}
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.html.sessions :as sessions]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.oauth :as oauth]
   [lambdaisland.ornament :as o]))


(o/defstyled home :div
  [sessions/session-card :mb-3]
  ([user]
   [:<>
    [:main
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]
     [sessions/session-card (sessions/rand-session)]]
    ]))
