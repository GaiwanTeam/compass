(ns co.gaiwan.compass.routes.sessions
  "We use 'session' as the group name for all kinds of things that people can
  participate in during a conference. Talks, workshops, activities like going
  climbing or yoga, BoF sessions, a concert, etc.

  'Activity' is generally reserved for fringe activities, many of which will be
  organized by participants.
  "
  (:require
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.sessions :as h]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.util :as util]
   [java-time.api :as time]))

(defn new-session [req]
  (if-not (:identity req)
    (util/redirect (oauth/flow-init-url {:redirect-url "/sessions/new"}))
    {:html/head [:title "Create new session"]
     :html/body [h/session-form {}]}))

(defn GET-session [req]
  {:html/body (pr-str (db/entity (parse-long (get-in req [:path-params :id]))))})

(defn params->session-data
  "convert the Http Post Params to data ready for DB transaction"
  [{:keys [title subtitle start-time end-time description
           type location
           capacity
           ticket-required? published?]}]
  (let [start    (time/zoned-date-time start-time db/event-time-zone)
        end      (time/zoned-date-time end-time db/event-time-zone)
        duration (time/duration start end)]
    (cond-> {:db/id "session"
             :session/title title
             :session/subtitle subtitle
             :session/time start
             :session/duration (str duration)
             :session/description description
             :session/type (keyword "session.type" type)
             :session/location (keyword "location.type" location)
             :session/capacity (Integer/parseInt capacity)}
      (= ticket-required? "on")
      (assoc :session/ticket-required? true)
      (= published? "on")
      (assoc :session/published? true))))

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
  (let [{:keys [tempids]} @(db/transact [(params->session-data params)])]
    (def tempids tempids)
    (util/redirect ["/sessions" (get tempids "session")]
                   {:flash "Successfully created!"})))

(defn routes []
  ["/sessions"
   [""
    {:name :activity/save
     :post {:handler save-session}}]
   ["/:id"
    {:get {:handler (fn [req]
                      (if (= "new" (get-in req [:path-params :id]))
                        (new-session req)
                        (GET-session req)))}}]
   ])
