(ns co.gaiwan.compass.routes.sessions
  "We use 'session' as the group name for all kinds of things that people can
  participate in during a conference. Talks, workshops, activities like going
  climbing or yoga, BoF sessions, a concert, etc.

  'Activity' is generally reserved for fringe activities, many of which will be
  organized by participants.
  "
  (:require
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.db.queries :as q]
   [co.gaiwan.compass.html.sessions :as session-html]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.model.assets :as assets]
   [co.gaiwan.compass.model.session :as session]
   [co.gaiwan.compass.model.user :as user]
   [java-time.api :as time]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.util :as util]
   [clojure.string :as str]))

(defn GET-session-new [req]
  (if-not (:identity req)
    {:status 200
     :headers {"HX-Trigger" "login-required"}} #_(util/redirect)
    {:html/head [:title "Create new session"]
     :html/body [session-html/session-form (:identity req)
                 nil
                 (q/all-session-types)]}))

(defn GET-session-edit [req]
  (let [session-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [session-html/session-form
                 (:identity req)
                 (q/session session-eid)
                 (q/all-session-types)]}))

(defn GET-session [req]
  (let [session-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [session-html/session-detail
                 (q/session session-eid)
                 (:identity req)]}))

(defn GET-session-card [req]
  (let [session-eid (parse-long (get-in req [:path-params :id]))]
    {:html/body [session-html/session-card
                 (q/session session-eid)
                 (:identity req)]}))

(defn params->session-data
  "convert the Http Post Params to data ready for DB transaction"
  [{:keys [title subtitle start-date start-time duration-time description
           type location
           capacity organizer-id
           ticket-required? published?
           image]}]
  (let [local-date (time/local-date start-date)
        local-time (time/local-time start-time)
        local-date-time (time/local-date-time local-date local-time)
        start    (time/zoned-date-time local-date-time db/event-time-zone)
        ;; end      (time/zoned-date-time end-time db/event-time-zone)
        duration (str "PT" duration-time "M")]
    (cond-> {:db/id "session"
             :session/title title
             :session/time start
             :session/duration duration
             :session/description description
             :session/type (or (some-> type parse-long) :session.type/activity)
             :session/location (or (some-> location parse-long) :location.type/hal5-long-table)
             :session/organized (parse-long organizer-id)
             :session/capacity (parse-long capacity)}

      subtitle
      (assoc :session/subtitle subtitle)
      (= ticket-required? "on")
      (assoc :session/ticket-required? true)
      (= published? "on")
      (assoc :session/published? true)
      image
      (assoc :session/image (assets/add-to-content-addressed-storage (:content-type image) (:tempfile image))
             :session/thumbnail (assets/add-to-content-addressed-storage "image/png" (assets/resize-image (:content-type image) (:tempfile image) 500 500))))))

(defn POST-create-session
  "Create new session, save to Datomic

  The typical params is:
  {:organizer-id \"455\"
   :name \"dsafa\",
   :description \"dsafa\",
   :type \"activity\",
   :location \"depot-main-stage\",
   :capacity \"34\",
   :ticket-required? \"on\"
   :published? \"on\"}"
  [{:keys [params]}]
  (let [{:keys [tempids]} @(db/transact [(doto (params->session-data params) prn)])]
    (response/redirect ["/sessions" (get tempids "session")]
                       {:flash "Successfully created!"})))

(defn PATCH-edit-session
  "Same as [[POST-create-session]], but edits an existing session."
  [{:keys [params path-params identity]}]
  (let [{:keys [id]} path-params
        id (parse-long id)
        session (q/session id)
        organizer-id (get-in session [:session/organized :db/id])]
    (if session
      (if (or (user/admin? identity)
              (= (:db/id identity) organizer-id))
        (do
          @(db/transact
            [(-> (params->session-data params)
                 (assoc :db/id id))])
          ;; TODO add a @everyone notification via discord/send-session-thread-message if there is a thread and something important changes
          {:location [:session/get {:id id}]
           :flash "Successfully edited!"})
        {:status 403
         :html/body [:p "You're not the organizer of this session."]})
      {:status 404
       :html/body [:p "Not found."]})))

(defn session-deleted-response [session-eid]
  {:location :sessions/index
   :hx/trigger (str "session-" session-eid "-deleted")})

(defn DELETE-session [{:keys [path-params identity]}]
  (let [session-eid (parse-long (:id path-params))]
    (when (session/organizing? (db/entity session-eid) identity)
      @(db/transact [[:db/retractEntity session-eid]])
      (session-deleted-response session-eid))))

(defn session-updated-response [session-eid]
  {:location :sessions/index
   :hx/trigger (str "session-" session-eid "-updated")})

(defn session-unchanged-response [session-eid]
  {:location :sessions/index
   :hx/trigger (str "session-" session-eid "-unchanged")})

(defn POST-participate
  "Add the user as a participant to an activity"
  [req]
  (let [user (:identity req)
        user-id (:db/id user)
        session-eid (parse-long (get-in req [:path-params :id]))
        session (q/session session-eid)
        capacity (:session/capacity session)
        signup-cnt (count (:session/participants session))]
    (cond
      ;; user leaves the session
      (session/participating? session user)
      (do
        @(db/transact [[:db/retract session-eid :session/participants user-id]])
        (when (:session/thread-id session)
          (discord/update-session-thread-member session-eid (:discord/id user) :remove))
        (session-updated-response session-eid))
      (< (or signup-cnt 0) capacity)
      ;; user participates the session
      (do
        @(db/transact [[:db/add session-eid :session/participants user-id]])
        (when (:session/thread-id session)
          (discord/update-session-thread-member session-eid (:discord/id user) :add))
        (session-updated-response session-eid))
      :else
      (session-unchanged-response session-eid))))

(defn POST-create-session-thread
  [{:keys [identity path-params]}]
  (let [session-eid (parse-long (:id path-params))
        session (q/session session-eid)]
    (if (or (user/admin? identity)
            (session/organizing? session identity))
      (if (:session/thread-id session)
        {:status 409
         :body "Session thread already exists"}
        (if (discord/create-session-thread session)
          (let [participant-ids
                (db/q '[:find [?id ...]
                        :in $ ?sid
                        :where
                        [?sid :session/participants ?pid]
                        [?pid :discord/id ?id]]
                      (db/db) session-eid)
                notif-msgs (->> participant-ids
                                (map discord/user-mention)
                                (map (partial format " %s "))
                                (util/partition-with-limit discord/message-limit)
                                (map str/join))]
            (discord/update-session-thread-member session-eid (:discord/id identity) :add)
            (doseq [msg notif-msgs]
              (discord/send-session-thread-message session-eid msg))
            {:location [:session/get {:id session-eid}]
             :flash "Thread created! You should have got a notification in Discord."})
          {:status 500
           :body "Thread could not be created"}))
      {:status 403
       :body "Missing permissions."})))

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
    ["/" {:name :sessions/index
          :get {:handler GET-sessions}}]]
   ["/sessions"
    {}
    [""
     {:name :session/save
      :post {:middleware [[response/wrap-requires-auth]]
             :handler POST-create-session}}]
    ["/new"
     {:name :session/new
      :get {:middleware [[response/wrap-requires-auth]]
            :handler GET-session-new}}]
    ["/:id"
     {:name :session/get
      :get {:handler GET-session}
      :delete {:middleware [[response/wrap-requires-auth]]
               :handler DELETE-session}
      :patch {:middleware [[response/wrap-requires-auth]]
              :handler PATCH-edit-session}}]
    ["/:id/edit"
     {:name :session/edit
      :get {:handler GET-session-edit}}]
    ["/:id/participate"
     {:name :session/participate
      :post {:middleware [[response/wrap-requires-auth]]
             :handler POST-participate}}]
    ["/:id/card"
     {:name :session/card
      :get {:handler GET-session-card}}]
    ["/:id/thread"
     {:name :session/create-thread
      :post {:middleware [[response/wrap-requires-auth]]
             :handler POST-create-session-thread}}]]])
