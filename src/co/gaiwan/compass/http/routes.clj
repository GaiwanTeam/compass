(ns co.gaiwan.compass.http.routes
  (:require
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.routes.sessions :as sessions]
   [co.gaiwan.compass.routes.home :as home]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]))

(defn routing-table []
  [(home/routes)
   (sessions/routes)
   (oauth/routes)])

;; - Sessions
;;   - Talk
;;   - Workshop
;;   - Activity
