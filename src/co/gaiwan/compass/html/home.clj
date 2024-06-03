(ns co.gaiwan.compass.html.home
  (:require
   [lambdaisland.ornament :as o]))


(o/defstyled home :main
  {:margin "0 auto"
   :max-width "1200px"
   }
  ([]
   [:main
    [:h1 "Compass"]
    [:p "Welcome to your Conference Compass!"]]))
