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

(defn wrap-login-check [handler]
  (fn [req]
    (log/trace ::login-check (:identity req))
    (if-let [user (:identity req)]
      (handler req)
      (util/redirect (oauth/flow-init-url {:redirect-url "/profiles"})))))

(defn GET-profile [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn GET-profile-form [req]
  {:html/body [h/profile-form
               (:identity req)]})

(defn params->profile-data
  [{:keys [name title user-id]}]
  {:db/id (parse-long user-id)
   :user/name name
   :user/title title})

(defn POST-save-profile
  "Save profile to DB
  
  The typical params is like:
  {:name \"Arne\"
   :tityle \"CEO of Gaiwan\"}"
  [{:keys [params]}]
  (let [{:keys [tempids]} @(db/transact [(params->profile-data params)])]
    (util/redirect ["/profiles"]
                   {:flash "Successfully Saved!"})))

(defn routes []
  ["/profiles"
   [""
    {:middleware [wrap-login-check]
     :get {:handler GET-profile}}]
   ["/edit"
    {:get {:handler GET-profile-form}}]
   ["/save"
    {:middleware [wrap-login-check]
     :post {:handler POST-save-profile}}]])
