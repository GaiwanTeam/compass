(ns co.gaiwan.compass.routes.sessions
  "We use 'session' as the group name for all kinds of things that people can
  participate in during a conference. Talks, workshops, activities like going
  climbing or yoga, BoF sessions, a concert, etc.

  'Activity' is generally reserved for fringe activities, many of which will be
  organized by participants.
  "
  (:require
   [co.gaiwan.compass.html.sessions :as h]
   [ring.util.response :as response]))

(defn new-session [req]
  (if-not (:identity req)
    (assoc (response/redirect "/")
           :flash "Please log in first")
    {:html/head [:title "Create new session"]
     :html/body [h/session-form {}]}))

(defn save-session [{:keys [params]}]
  ;; TODO save to datomic
  {:html/body [:p "OK " (pr-str params)]})

(defn routes []
  ["/sessions"
   [""
    {:name :activity/save
     :post {:handler save-session}}]
   ["/new"
    {:name :activity/new
     :get {:handler new-session}}]]
  )
