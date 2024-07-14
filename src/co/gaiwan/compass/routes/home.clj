(ns co.gaiwan.compass.routes.home
  "Front page routes and business logic"
  (:require
   [co.gaiwan.compass.html.home :as h]
   [co.gaiwan.compass.db :as db]))

(defn all-sessions []
  (db/q
   '[:find
     [
      (pull ?e [* {:session/type [*]
                   :session/location [*]}]) ...]
     :where
     [?e :session/title]]
   (db/db)))

(defn GET-home [req]
  {:html/head [:title "Conference Compass"]
   :html/body [h/home {:user (:identity req)
                       :sessions (all-sessions)}]})
