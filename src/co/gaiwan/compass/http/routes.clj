(ns co.gaiwan.compass.http.routes
  (:require
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.routes.sessions :as sessions]
   [co.gaiwan.compass.routes.home :as home]
   [co.gaiwan.compass.routes.meta :as meta]
   [hato.client :as hato]
   [lambdaisland.uri :as uri]))

(defn routing-table []
  [(meta/routes)
   (home/routes)
   (sessions/routes)
   (oauth/routes)
   ["/fail" {:get {:handler (fn [_] (throw (ex-info "fail" {:fail 1})))}}]])

;; - Sessions
;;   - Talk
;;   - Workshop
;;   - Activity
