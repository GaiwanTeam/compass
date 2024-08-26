(ns co.gaiwan.compass.routes.admin
  (:require [co.gaiwan.compass.db :as db]
            [co.gaiwan.compass.model.user :as user]
            [co.gaiwan.compass.util :as util]))

(defn wrap-admin-only [handler]
  (fn [req]
    (if (user/admin? (:identity req))
      (handler req)
      {:status 403
       :html/body [:p "Admin only! Make sure you have a registered crew ticket."]})))

(defn all-users []
  (map db/entity
       (db/q '[:find [?e ...]
               :where [?e :user/uuid]]
             (db/db))))

(defn GET-users [req]
  {:html/body
   [:pre
    (util/pprint-str (for [u (all-users)]
                       (into (if-let [t  (user/assigned-ticket u)]
                               {:tito/ticket (into {} t)}
                               {})
                             u)))]})

(defn routes []
  ["/admin" {:middleware [wrap-admin-only]}
   ["/users" {:get {:handler #'GET-users}}]])
