(ns co.gaiwan.compass.model.user
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.model.assets :as assets]
   [hato.client :as hato])
  )

(defn avatar-css-value [user]
  (if-let [url (:public-profile/avatar-url user)]
    (str "url(" (assets/image-url url) ")")
    (str "var(--gradient-" (inc (mod (:db/id user) 7)) ")")))

(defn assigned-ticket [user]
  (first (:tito.ticket/_assigned-to user)))

(defn admin? [user]
  (when-let [ticket (assigned-ticket user)]
    (= "crew" (:tito.release/slug (:tito.ticket/release ticket)))))
