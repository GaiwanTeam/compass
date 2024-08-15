(ns co.gaiwan.compass.routes.sessions
  "We use 'session' as the group name for all kinds of things that people can
  participate in during a conference. Talks, workshops, activities like going
  climbing or yoga, BoF sessions, a concert, etc.

  'Activity' is generally reserved for fringe activities, many of which will be
  organized by participants.
  "
  (:require
   [clojure.java.io :as io]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.db.queries :as q]
   [co.gaiwan.compass.html.sessions :as session-html]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.model.session :as session]
   [co.gaiwan.compass.util :as util]
   [java-time.api :as time]))

(defn GET-session-new [req]
  (if-not (:identity req)
    {:status 200
     :headers {"HX-Trigger" "login-required"}} #_(util/redirect)
    {:html/head [:title "Create new session"]
     :html/body [session-html/session-form {}]}))

(defn GET-session [req]
  (let [session-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [session-html/session-detail
                 (db/entity session-eid)
                 (:identity req)]}))

(defn GET-session-card [req]
  (let [session-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [session-html/session-card
                 (db/pull '[* {:session/type [*]
                               :session/location [*]
                               :session.type [*]}] session-eid)
                 (:identity req)]}))

(defn params->session-data
  "convert the Http Post Params to data ready for DB transaction"
  [{:keys [title subtitle start-date start-time duration-time description
           type location
           capacity
           ticket-required? published?]
    :or {type "activity"}}
   identity]
  (let [local-date (time/local-date start-date)
        local-time (time/local-time start-time)
        local-date-time (time/local-date-time local-date local-time)
        start    (time/zoned-date-time local-date-time db/event-time-zone)
        ;; end      (time/zoned-date-time end-time db/event-time-zone)
        duration (str "PT" duration-time "M")
        _ (prn :debug-duration duration)]
    (cond-> {:db/id "session"
             :session/title title
             :session/organized (:db/id identity)
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

(defn POST-save-session
  "Create new session, save to Datomic

  The typical params is:
  {:name \"dsafa\",
   :description \"dsafa\",
   :type \"activity\",
   :location \"depot-main-stage\",
   :capacity \"34\",
   :ticket-required? \"on\"
   :published? \"on\"}"
  [{:keys [params identity]}]
  (let [{:keys [tempids]} @(db/transact [(params->session-data params identity)])]
    (when (:image params)
      (let [{:keys [filename tempfile]} (:image params)
            session-eid (get tempids "session")
            file-path  (str (config/value :uploads/dir) "/" session-eid "_" filename)]
        (io/copy tempfile (io/file file-path))
        @(db/transact [{:db/id (get tempids "session")
                        :session/image (str "/" file-path)}])))
    (util/redirect ["/sessions" (get tempids "session")]
                   {:flash "Successfully created!"})))

(defn DELETE-session [{:keys [path-params identity]}]
  (let [session-eid (parse-long (:id path-params))]
    ;; FIXME: orga should also be able to delete
    (when (= (:db/id identity) (-> session-eid db/entity :session/organized :db/id))
      @(db/transact [[:db.fn/retractEntity session-eid]])
      (util/redirect "/"))))

(defn session-updated-response [session-eid]
  {:status 200
   :headers {"HX-Trigger" (str "session-" session-eid "-updated")}
   :body ""})

(defn session-unchanged-response [session-eid]
  {:status 200
   :headers {"HX-Trigger" (str "session-" session-eid "-unchanged")}
   :body ""})

(defn POST-participate
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
            capacity (:session/capacity session)
            signup-cnt (:session/signup-count session)]
        (cond
          ;; user leaves the session
          (session/participating? session user)
          (do @(db/transact [[:db/cas session-eid :session/signup-count signup-cnt (dec signup-cnt)]
                             [:db/retract session-eid :session/participants user-id]])
              (session-updated-response session-eid))
          (< (or signup-cnt 0) capacity)
          ;; user participates the session
          (do
            @(db/transact [[:db/cas session-eid :session/signup-count signup-cnt ((fnil inc 0) signup-cnt)]
                           [:db/add session-eid :session/participants user-id]])
            (session-updated-response session-eid))
          :else
          (session-unchanged-response session-eid)))
      #_{:html/body (pr-str (db/entity (parse-long (get-in req [:path-params :id]))))})
    (util/redirect (oauth/flow-init-url {:redirect-url (str "/sessions/" (get-in req [:path-params :id]) "/participate")}))))

(defn GET-sessions [req]
  (let [filters  (-> req :session :session-filters)
        sessions (q/all-sessions)
        user     (:identity req)]
    {:html/body
     [session-html/session-list+filters
      {:filters  filters
       :user     user
       :sessions (session/apply-filters sessions user filters)}]}))

(defn routes []
  [[""
    ["/" {:get {:handler GET-sessions}}]]
   ["/sessions"
    [""
     {:name :activity/save
      :get {:handler GET-sessions}
      :post {:handler POST-save-session}}]
    ["/new"
     {:get {:handler GET-session-new}}]
    ["/:id"
     {:get {:handler GET-session}
      :delete {:handler DELETE-session}}]
    ["/:id/participate"
     {:post {:handler POST-participate}}]
    ["/:id/card"
     {:get {:handler GET-session-card}}]]])
