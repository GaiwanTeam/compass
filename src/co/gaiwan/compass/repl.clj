(ns co.gaiwan.compass.repl
  (:require
   [co.gaiwan.compass.db :as db]
   [clojure.java.browse :refer [browse-url]]))

(defn user [name-or-email]
  (db/q '[:find (pull ?e [*])
          :in $ ?n-e
          :where
          [?e :public-profile/name ?n]
          [?e :discord/email ?m]
          [(.contains ?n ?n-e)
           ]]
        (db/db)
        name-or-email
        ))
