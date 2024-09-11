(ns repl-sessions.participants
  "Prepare some testing data for showing participants in a session.

   How to use:
   1. You create a session and get the session id from the URL 
   2. Run this participants namespace to create another 10 temp users
   3. Go to the sessions/:id page to do the testing
  "
  (:require
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.model.assets :as assets]
   [datomic.api :as d]))

(def new-session-eid
  17592186045520)
;; Avatar source URL https://github.com/alohe/avatars
(defn temp-user-tx
  " Create the user txes

    1. Download some testing avatar image from remote URL
    2. Create 10 testing users with temp name adn temp avatar
    3. Build participating relationship with session: `eid`"
  [eid]
  (let [avatar-url-part "https://cdn.jsdelivr.net/gh/alohe/avatars/png/vibrent_"]
    (concat
     (mapv
      (fn [x]
        {:db/id (str "temp-" x)
         :discord/email (str "temp-email-" x "@gaiwan.co")
         :public-profile/name (str "temp-user-" x)
         :public-profile/avatar-url (assets/download-image (str avatar-url-part x ".png"))})
      (range 1 11))
     ;; Below is for session join
     (mapv
      (fn [x]
        {:db/id eid
         :session/participants (str "temp-" x)})
      (range 1 11)))))

(def tx (temp-user-tx
         new-session-eid))

(db/transact tx)
