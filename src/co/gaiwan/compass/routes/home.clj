(ns co.gaiwan.compass.routes.home
  (:require
   [co.gaiwan.compass.html.home :as home]
   [co.gaiwan.compass.html.layout :as layout]
   [lambdaisland.hiccup :as hiccup]))

(defn GET-home [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (hiccup/render
          [layout/layout
           [home/home]
           {:title "Conference Compass"}])})
