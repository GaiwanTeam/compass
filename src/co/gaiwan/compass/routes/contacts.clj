(ns co.gaiwan.compass.routes.contacts
  (:require
   [clj.qrgen :as qr]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.db.queries :as q]
   [co.gaiwan.compass.html.contacts :as h]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.assets :as assets]
   [co.gaiwan.compass.model.attendees :as attendees]
   [ring.util.response :as ring-response]))

(defn GET-contact-list
  "Show the private contact list of the user.
    - Users can revoke their contacts in this page"
  [req]
  {:html/body [h/contact-detail
               (:identity req)]})

(defn eid->qr-hash
  "create an uuid as the hash for eid to prevent guessing
   store this uuid in the user"
  [user-eid]
  (let [qr-hash (random-uuid)]
    @(db/transact [{:db/id user-eid
                    :user/hash qr-hash}])
    qr-hash))

(defn qr-hash->eid
  "Accept a uuid type's qr-hash, and use it to query the
   user's eid"
  [qr-hash]
  (db/q '[:find ?e .
          :in $ ?hash
          :where
          [?e :user/hash ?hash]]
        (db/db) qr-hash))

(defn GET-qr-html [req]
  {:html/body [:div
               {:style {:margin "var(--size-4)"}}
               [:h2 "Add Contact"]
               [:img {:src (url-for :contact/qr-png)}]]
   :html/layout false})

(defn GET-qr-code
  [{:keys [identity] :as req}]
  (let [user-eid (:db/id identity)
        host (config/value :compass/origin)
        qr-hash (str (eid->qr-hash user-eid))
        url (str host (url-for :contact/add {:qr-hash qr-hash}))
        qr-image (qr/as-bytes (qr/from url :size [400 400]))]
    (-> (ring-response/response qr-image)
        (assoc-in [:headers "content-type"] "image/png"))))

(defn GET-attendees [req]
  (let [attendees (q/all-users)]
    {:html/body
     [:<>
      [:p "The Attendees List"]
      (for [atd (attendees/user-list attendees)]
        (h/attendee-card atd))]}))

(defn GET-contact
  [req]
  {:html/body
   [:div
    [:a
     {:href (url-for :profile/index)
      :style {:display "none"}
      :hx-trigger "contact-added from:body"}]
    [:button
     {:hx-post (url-for :contact/add {:qr-hash (get-in req [:path-params :qr-hash])})}
     (str "Accept invite")]]})

(defn DELETE-contact
  [req]
  (let [me-id (:db/id (:identity req))
        contact-id (parse-long (get-in req [:path-params :id]))]
    @(db/transact [[:db/retract me-id :user/contacts contact-id]
                   [:db/retract contact-id :user/contacts me-id]])
    {:location :contacts/index
     :hx/trigger "contact-deleted"}))

(defn POST-contact
  "Part of the url is hash of the contact's user eid
   Decode it and add that contact"
  [{:keys [identity] :as req}]
  (let [user-eid (:db/id identity)
        qr-hash (parse-uuid (get-in req [:path-params :qr-hash]))
        contact-eid (qr-hash->eid qr-hash)
        ;; According to the schema
        ;; A :u/c B means that user A agrees to show their public profile to user B.
        ;; contact -> A
        ;; user -> B
        _ @(db/transact [{:db/id contact-eid
                          :user/contacts user-eid}
                         {:db/id user-eid
                          :user/contacts contact-eid}])]
    {:location :contact/add
     :hx/trigger "contact-added"}))

(defn routes []
  [
   ["/contact"
    {:middleware [[response/wrap-requires-auth]]}
    ["/qr" {:name :contact/qr
            :get {:handler GET-qr-html}}]
    ["/qr.png" {:name :contact/qr-png
                :get {:handler GET-qr-code}}]
    ["/:qr-hash"
     {:name :contact/add
      :post       {:handler POST-contact}
      :get        {:handler GET-contact}}]
    ["/link/:id"
     {:name :contact/link
      :delete     {:handler DELETE-contact}}]]
   ["/contacts"
    {:middleware [[response/wrap-requires-auth]]}
    ["/" {:name :contacts/index
          :get {:handler GET-contact-list}}]]
   #_
   ["/attendees"
    [""
     {:name :attendees/index
      :middleware [[response/wrap-requires-auth]]
      :get        {:handler GET-attendees}}]]])
