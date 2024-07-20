(ns co.gaiwan.compass.routes.home
  "Front page routes and business logic"
  (:require
   [lambdaisland.uri :as uri]
   [clojure.string :as str]
   [co.gaiwan.compass.html.home :as h]
   [co.gaiwan.compass.db :as db]))

(defn all-sessions
  [{:keys [location type]}]
  (->> (db/q
        '[:find
          [(pull ?e [* {:session/type [*]
                        :session/location [*]}]) ...]
          :where
          [?e :session/title]]
        (db/db))
       (sort-by :session/time)
       (filter #(if (= type "all")
                  true
                  (= (keyword "session.type" type)
                     (get-in % [:session/type :db/ident]))))
       (filter #(if (= location "all")
                  true
                  (= (keyword "location.type" location)
                     (get-in % [:session/location :db/ident]))))))

(defn GET-home [req]
  {:html/head [:title "Conference Compass"]
   :html/body [h/home {:user (:identity req)
                       :sessions (all-sessions {:type "all"
                                                :location "all"})}]})

(defn GET-filters-showed [req]
  {:html/head [:title "filter snippets"]
   :html/body [h/filters-showed]})

(defn GET-filters-hidden [req]
  {:html/head [:title "filter snippets"]
   :html/body [h/filters-hidden]})

(defn GET-conf-sessions [req]
  (let [qs-m (uri/query-string->map (:query-string req))]
    {:html/head [:title "sessions"]
     :html/body [h/session-snippet (all-sessions qs-m)]}))

(defn routes []
  [""
   ["/"
    {:name :index
     :get {:handler GET-home}}]
   ["/show-filters"
    {:get {:handler GET-filters-showed}}]
   ["/hide-filters"
    {:get {:handler GET-filters-hidden}}]
   ["/conf-sessions"
    {:get {:handler GET-conf-sessions}}]])
