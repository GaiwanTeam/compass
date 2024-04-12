(ns co.gaiwan.compass.http.routes
  (:require
   [co.gaiwan.compass.routes.home :as home]))

(defn routing-table []
  ["/"
   {:name :index
    :get {:handler home/GET-home}}])
