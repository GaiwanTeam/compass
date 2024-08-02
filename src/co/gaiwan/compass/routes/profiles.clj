(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.profiles :as h]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.util :as util]
   [io.pedestal.log :as log]
   [java-time.api :as time]))

(defn GET-profile [req]
  (let [profile-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [h/profile-detail
                 (db/entity profile-eid)
                 (:identity req)]}))

(defn routes []
  ["/profiles"
   ["/:id"
    {:get {:handler (fn [req]
                      (GET-profile req))}}]])
