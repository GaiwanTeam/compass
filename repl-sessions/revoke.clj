(ns repl-sessions.revoke
  "Prepare some testing data for revoke contacts feature

   How to use:
   1. You first login by Discord
   2. Run this revoke namespace to create another 10 temp users
   3. Go to the /profile page to do the revoke testing
  "
  (:require
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.model.assets :as assets]
   [datomic.api :as d]))

(defn test-user-eid
  "Query the database to find out a certain login by discord user"
  [user-name]
  (db/q
   '[:find ?e .
     :in $ ?u
     :where
     [?e :public-profile/name ?u]]
   (db/db) user-name))

;; Avatar source URL https://github.com/alohe/avatars
(defn temp-user-tx
  " Create the user txes

    1. Download some testing avatar image from remote URL
    2. Create 10 testing users with temp name adn temp avatar
    3. Build contacts relationship with user: `eid`"
  [eid]
  (let [avatar-url-part "https://cdn.jsdelivr.net/gh/alohe/avatars/png/vibrent_"]
    (concat
     (mapv
      (fn [x]
        {:db/id (str "temp-" x)
         :user/contacts eid
         :public-profile/name (str "temp-user-" x)
         :public-profile/avatar-url (assets/download-image (str avatar-url-part x ".png"))})
      (range 1 11))
     (mapv
      (fn [x]
        {:db/id eid
         :user/contacts (str "temp-" x)})
      (range 1 11)))))

(def tx (temp-user-tx
         (test-user-eid "Arne")))

(db/transact tx)
