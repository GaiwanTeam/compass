(ns co.gaiwan.compass.http.routes
  "Combined HTTP routing table"
  (:require
   [co.gaiwan.compass.routes.admin :as admin]
   [co.gaiwan.compass.routes.contacts :as contacts]
   [co.gaiwan.compass.routes.filters :as filters]
   [co.gaiwan.compass.routes.meta :as meta]
   [co.gaiwan.compass.routes.oauth :as oauth]
   [co.gaiwan.compass.routes.profiles :as profiles]
   [co.gaiwan.compass.routes.sessions :as sessions]
   [co.gaiwan.compass.routes.ticket :as ticket]))

(defn routing-table []
  [(meta/routes)
   (sessions/routes)
   (profiles/routes)
   (contacts/routes)
   (oauth/routes)
   (filters/routes)
   (ticket/routes)
   (admin/routes)
   ["/fail" {:get {:handler (fn [_] (throw (ex-info "fail" {:fail 1})))}}]])

;; - Sessions
;;   - Talk
;;   - Workshop
;;   - Activity
