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

