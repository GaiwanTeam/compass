(ns repl-sessions.db
  (:require
   [co.gaiwan.compass.db :as db]
   [datomic.api :as d]))

(db/q
 '[:find
   [(pull ?e [* {:session/type [*]
                 :session/location [*]}]) ...]
   :where
   [?e :session/title]]
 (db/db))
(deref
 (db/transact [[:db/retractEntity 17592186045468]]))
(user/conn)

(db/transact  [{:user/email "aa@gmail.com"
                :user/handle "laurence.chen"
                :user/name "Laurence"}])

(db/q
 '[:find
   [(pull ?e [*]) ...]
   :where
   [?e :user/email]]
 (db/db))

(def session-eid 17592186045438)

(db/transact [{:db/id session-eid
               :session/participants  17592186045458}])

(first (:session/participants (db/entity session-eid)))
;; Test transact participants

(def req {:identity {:user/email "ddd"}
          :path-params {:id "17592186045438"}})

(def session-eid 17592186045438)

@(db/transact [[:db/add session-eid :session/capacity 14]
               [:db/add session-eid :session/signup 0]])

(def session (db/entity (parse-long (get-in req [:path-params :id]))))

(let [user-id-str (:user/email (:identity req))
      session-eid (parse-long (get-in req [:path-params :id]))
      ;; session (db/entity session-eid)
      capacity (:session/capacity session)
      curr-participants (:session/participants session)
      signup (:session/signup session)
      new-signup (inc signup)]
        ;;TODO 
        ;; Write some code to handle the case that :db/cas throws exception at race condition
  (prn :session-eid session-eid)
  (prn :capacity capacity)
  (prn :curr-ps curr-participants)
  (prn :check (< signup capacity))
  (prn :debug-tx [[:db/cas session-eid :session/signup signup new-signup]
                  [:db/add session-eid :session/participants user-id-str]])
  (if (< signup capacity)
    @(db/transact [[:db/cas session-eid :session/signup signup new-signup]
                   [:db/add session-eid :session/participants user-id-str]])
    {:html/body "No enough capacity for this session"}))
