(ns co.gaiwan.compass.routes.sessions
  "We use 'session' as the group name for all kinds of things that people can
  participate in during a conference. Talks, workshops, activities like going
  climbing or yoga, BoF sessions, a concert, etc.

  'Activity' is generally reserved for fringe activities, many of which will be
  organized by participants.
  "
  (:require
   [co.gaiwan.compass.html.sessions :as h]
   [ring.util.response :as response]
   [co.gaiwan.compass.db :as db]))

(defn new-session [req]
  (if-not (:identity req)
    (assoc (response/redirect "/")
           :flash "Please log in first")
    {:html/head [:title "Create new session"]
     :html/body [h/session-form {}]}))

(defn params->session-data
  "convert the Http Post Params to data ready for DB transaction"
  [{:keys [name description
           type location
           capacity
           ticket-required? published?]}]
  (cond-> {:session/title name
           :session/description description
           :session/type (keyword "session.type" type)
           :session/location (keyword "location.type" location)
           :session/capacity (Integer/parseInt capacity)}
    (= ticket-required? "on")
    (assoc :session/ticket-required? true)
    (= published? "on")
    (assoc :session/published? true)))

(defn save-session
  "Save session to Datomic
  
  The typical params is:
  {:name \"dsafa\",
   :description \"dsafa\",
   :type \"activity\", 
   :location \"depot-main-stage\",
   :capacity \"34\", 
   :ticket-required? \"on\"
   :published? \"on\"}"
  [{:keys [params]}]
  (db/transact [(params->session-data params)])
  {:html/body [:p "OK " (pr-str params)]})

(defn routes []
  ["/sessions"
   [""
    {:name :activity/save
     :post {:handler save-session}}]
   ["/new"
    {:name :activity/new
     :get {:handler new-session}}]])
