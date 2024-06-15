(ns co.gaiwan.compass.http.routes
  (:require
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.routes.home :as home]
   [co.gaiwan.compass.http.oauth :as oauth]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]))

(defn routing-table []
  [["/"
    {:name :index
     :get {:handler home/GET-home}}]
   (oauth/routes)])

