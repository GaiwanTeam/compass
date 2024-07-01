(ns co.gaiwan.compass.http.routes
  (:require
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.routes.sessions :as sessions]
   [co.gaiwan.compass.routes.home :as home]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]))

(defn routing-table []
  [["/"
    {:name :index
     :get {:handler home/GET-home}}]
   (sessions/routes)
   (oauth/routes)])

;; - Sessions
;;   - Talk
;;   - Workshop
;;   - Activity
