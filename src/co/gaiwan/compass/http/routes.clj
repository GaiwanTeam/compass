(ns co.gaiwan.compass.http.routes
  "Combined HTTP routing table"
  (:require
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.routes.filters :as filters]
   [co.gaiwan.compass.routes.meta :as meta]
   [co.gaiwan.compass.routes.profiles :as profiles]
   [co.gaiwan.compass.routes.sessions :as sessions]))

(defn routing-table []
  [(meta/routes)
   (sessions/routes)
   (profiles/routes)
   (oauth/routes)
   (filters/routes)
   ["/fail" {:get {:handler (fn [_] (throw (ex-info "fail" {:fail 1})))}}]])

;; - Sessions
;;   - Talk
;;   - Workshop
;;   - Activity
