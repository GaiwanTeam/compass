(ns co.gaiwan.compass.routes.home
  (:require
   [co.gaiwan.compass.html.home :as h]))

(defn GET-home [req]
  {:html/head [:title "Conference Compass"]
   :html/body [h/home (:identity req)]})
