(ns co.gaiwan.compass.routes.sessions
  "We use 'session' as the group name for all kinds of things that people can
  participate in during a conference. Talks, workshops, activities like going
  climbing or yoga, BoF sessions, a concert, etc.

  'Activity' is generally reserved for fringe activities, many of which will be
  organized by participants.
  "
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.sessions :as h]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.util :as util]
   [io.pedestal.log :as log]
   [java-time.api :as time]))

(defn new-session [req]
  (if-not (:identity req)
    (util/redirect (oauth/flow-init-url {:redirect-url "/sessions/new"}))
    {:html/head [:title "Create new session"]
     :html/body [h/session-form {}]}))

(defn GET-session [req]
  (let [session-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [h/session-detail
                 (db/entity session-eid)
                 (:identity req)]}))

(defn duration-string-to-iso8601
  "Convert \"03:00\" to iso8601 format \"PT3H\" "
  [duration-str]
  (let [[hours minutes] (map #(Integer/parseInt %) (str/split duration-str #":"))
        hours-str (if (pos? hours) (str "PT" hours "H") "")
        minutes-str (if (pos? minutes) (str minutes "M") "")]
    (str hours-str minutes-str)))

(defn params->session-data
  "convert the Http Post Params to data ready for DB transaction"
  [{:keys [title subtitle start-date start-time duration-time description
           type location
           capacity
           ticket-required? published?]}]
  (let [local-date (time/local-date start-date)
        local-time (time/local-time start-time)
        local-date-time (time/local-date-time local-date local-time)
        start    (time/zoned-date-time local-date-time db/event-time-zone)
        ;; end      (time/zoned-date-time end-time db/event-time-zone)
        duration (duration-string-to-iso8601 duration-time)
        _ (prn :debug-duration duration)]
    (cond-> {:db/id "session"
             :session/title title
             :session/subtitle subtitle
             :session/time start
             :session/duration duration
             :session/description description
             :session/type (keyword "session.type" type)
             :session/location (keyword "location.type" location)
             :session/signup-count 0
             :session/capacity (parse-long capacity)}
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
    (util/redirect ["/sessions" (get tempids "session")]
                   {:flash "Successfully created!"})))

(defn session-card-response [session user]
  {:html/layout false
   :html/body [h/session-card session user]})

(defn participate-session
  ""
  [req]
  (if-let [user (:identity req)]
    (do
      (let [user-id (:db/id user)
            session-eid (parse-long (get-in req [:path-params :id]))
            session-seletor '[* {:session/type [*]
                                 :session/location [*]}]
            pull-session #(db/pull session-seletor session-eid)
            session (pull-session)
            participants (->> session
                              :session/participants
                              (map :db/id)
                              set)
            capacity (:session/capacity session)
            signup-cnt (:session/signup-count session)]
        (cond
          (participants user-id)
          (do @(db/transact [[:db/cas session-eid :session/signup-count signup-cnt (dec signup-cnt)]
                             [:db/retract session-eid :session/participants user-id]])
              (session-card-response (pull-session) user))
          (< (or signup-cnt 0) capacity)
          (do
            ;;TODO: add try/catch to handle :db/cas
            @(db/transact [[:db/cas session-eid :session/signup-count signup-cnt ((fnil inc 0) signup-cnt)]
                           [:db/add session-eid :session/participants user-id]])
            (session-card-response (pull-session) user))
          :else
          (session-card-response session user)))
      #_{:html/body (pr-str (db/entity (parse-long (get-in req [:path-params :id]))))})
    (util/redirect (oauth/flow-init-url {:redirect-url (str "/sessions/" (get-in req [:path-params :id]) "/participate")}))))

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
   ["/:id/participate"
    {:post {:handler participate-session}}]])
