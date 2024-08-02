(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.profiles :as h]
   [co.gaiwan.compass.routes.oauth :as oauth]
   [co.gaiwan.compass.util :as util]
   [io.pedestal.log :as log]
   [java-time.api :as time]))

(defn GET-profile [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn GET-profile-form [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn save-profile [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn routes []
  ["/profiles"
   [""
    {:get {:handler GET-profile}}]
   ["/edit"
    {:get {:handler GET-profile-form}}]
   ["/save"
    {:post {:handler save-profile}}]])
