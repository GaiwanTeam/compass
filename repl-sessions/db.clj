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

;; Test transact participants

(def req {:identity "ccc"
          :path-params {:id "17592186045455"}})
(def session (merge {:session/capacity 14}
                    (db/entity (parse-long (get-in req [:path-params :id])))))

(let [user-id-str (:identity req)
      session-eid (parse-long (get-in req [:path-params :id]))
      ;; session (db/entity session-eid)
      capacity (:session/capacity session)
      curr-participants (:session/participants session)]
        ;;TODO 
        ;; Write some code to handle the case that :db/cas throws exception at race condition
  (prn :session-eid session-eid)
  (prn :capacity capacity)
  (prn :curr-ps curr-participants)
  (prn :check (< (count curr-participants) capacity))
  (if (< (count curr-participants) capacity)
    @(db/transact [[:db/add session-eid :session/participants user-id-str]])
    {:html/body "No enough capacity for this session"}))
