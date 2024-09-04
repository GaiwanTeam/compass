(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clj.qrgen :as qr]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.db.queries :as q]
   [co.gaiwan.compass.html.profiles :as h]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.assets :as assets]
   [co.gaiwan.compass.model.attendees :as attendees]
   [ring.util.response :as ring-response]))

(defn GET-profile [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn GET-profile-form [req]
  {:html/body [h/profile-form
               (:identity req)]})

(defn GET-private-name [{:keys [params] :as req}]
  {:html/body [h/private-name
               (:identity req)
               params]})

(defn GET-link [{:keys [params] :as req}]
  {:html/body [h/links-table
               {}
               params]})

(defn index->link-data
  [{:keys [user-id] :as params} idx]
  (let [user-id (parse-long user-id)
        private-profile-links (set (db/q '[:find [?e ...]
                                           :in $ ?p
                                           :where [?p :private-profile/links ?e]]
                                         (db/db) user-id))
        ;;_ (tap> {:private-links private-profile-links})
        public-profile-links (set (db/q '[:find [?e ...]
                                          :in $ ?p
                                          :where [?p :public-profile/links ?e]]
                                        (db/db) user-id))
        ;; _ (tap> {:public-links public-profile-links})
        link-id-key (keyword (str "link-id-" idx))
        href-key (keyword (str "link-ref-" idx))
        type-key (keyword (str "link-type-" idx))
        private-link-key (keyword (str "private-" idx))
        public-link-key (keyword (str "public-" idx))
        link-id-val (link-id-key params)
        link-id-val (when link-id-val (parse-long link-id-val))
        href-val (href-key params)
        type-val (type-key params)
        private-link-val (private-link-key params)
        public-link-val (public-link-key params)
        profile-data {:db/id (if link-id-val
                               link-id-val
                               (str "temp-" idx))
                      :profile-link/user user-id
                      :profile-link/type type-val
                      :profile-link/href href-val}]
    (tap> {:cond1 [link-id-val (private-profile-links link-id-val) (nil? private-link-val)]
           :cond2 [link-id-val (nil? (private-profile-links link-id-val)) private-link-val]
           :cond3 [link-id-val (public-profile-links link-id-val) (nil? public-link-val)]
           :cond4 [link-id-val (nil? (public-profile-links link-id-val)) public-link-val]})
    (cond-> [profile-data]
      ;; new profile-link
      (and (nil? link-id-val) private-link-val)
      (conj [:db/add user-id :private-profile/links (str "temp-" idx)])
      (and (nil? link-id-val) public-link-val)
      (conj [:db/add user-id :public-profile/links (str "temp-" idx)])
      ;; existing proflie-link
      (and link-id-val (private-profile-links link-id-val) (nil? private-link-val))
      (conj [:db/retract user-id :private-profile/links link-id-val])
      (and link-id-val (nil? (private-profile-links link-id-val)) private-link-val)
      (conj [:db/add user-id :private-profile/links link-id-val])
      (and link-id-val (public-profile-links link-id-val) (nil? public-link-val))
      (conj [:db/retract user-id :public-profile/links link-id-val])
      (and link-id-val (nil? (public-profile-links link-id-val)) public-link-val)
      (conj [:db/add user-id :public-profile/links link-id-val]))))

(defn params->profile-data
  [{:keys [user-id hidden?
           private-name-switch
           bio_public name_public
           bio_private name_private
           rows-count
           image] :as params}]
  (tap> params)
  (let [user-id (parse-long user-id)]
    (cond-> (into [{:db/id user-id
                    :public-profile/bio bio_public
                    :public-profile/name name_public
                    :public-profile/hidden? (= "on" hidden?)
                    :private-profile/bio bio_private}]
                  (mapcat #(index->link-data params %))
                  (range (parse-long rows-count)))
      image
      (conj [:db/add user-id :public-profile/avatar-url
             (assets/add-to-content-addressed-storage (:content-type image) (:tempfile image))])

      (and (= "on" private-name-switch)
           (not (str/blank? name_private)))
      (conj [:db/add user-id :private-profile/name name_private])

      (or (not= "on" private-name-switch)
          (str/blank? name_private))
      (conj [:db/retract user-id :private-profile/name]))))

(defn POST-save-profile
  "Save profile to DB

  The typical params is like:
  {:name \"Arne\"
   :tityle \"CEO of Gaiwan\"
   :image {:content-type :filename :size :tempfile}}"
  [{:keys [params identity] :as req}]
  @(db/transact (params->profile-data params))
  (response/redirect "/profile"
                     {:flash "Successfully Saved!"}))

(defn file-handler [req]
  (let [file (io/file (config/value :uploads/dir) (get-in req [:path-params :filename]))]
    (if (.exists file)
      (ring-response/file-response (.getPath file))
      (ring-response/not-found "File not found"))))

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
               [:h2 "Add Contact"]
               [:img {:src (url-for :contact/qr-png)}]]
   :html/layout false})

(defn GET-qr-code
  [{:keys [identity] :as req}]
  (let [user-eid (:db/id identity)
        host (config/value :compass/origin)
        qr-hash (eid->qr-hash user-eid)
        url (str host "/attendees/" qr-hash)
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

(defn GET-contact [req]
  {:html/body [:button {:hx-post (url-for :contact/add {:uuid "123"})}]})

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
    (response/redirect "/profile"
                       {:flash "Successfully Saved!"})))

(defn routes []
  [["/profile"
    [""
     {:name :profile/index
      :middleware [[response/wrap-requires-auth]]
      :get        {:handler GET-profile}}]
    ["/edit"
     {:name :profile/edit
      :get {:handler GET-profile-form}}]
    ["/edit/private-name"
     {:name :profile/private-name
      :get {:handler GET-private-name}}]
    ["/edit/link"
     {:name :profile/add-link
      :get {:handler GET-link}}]
    ["/save"
     {:middleware [[response/wrap-requires-auth]]
      :post       {:handler POST-save-profile}}]]
   ["/uploads/:filename"
    {:middleware [[response/wrap-requires-auth]]
     :get        {:handler file-handler}}]
   ["/contact"
    {:middleware [[response/wrap-requires-auth]]}
    ["/qr" {:name :contact/qr
            :get {:handler GET-qr-html}}]
    ["/qr.png" {:name :contact/qr-png
                :get {:handler GET-qr-code}}]]
   ["/attendees"
    [""
     {:name :attendees/index
      :middleware [[response/wrap-requires-auth]]
      :get        {:handler GET-attendees}}]
    ["/:qr-hash"
     {:middleware [[response/wrap-requires-auth]]
      :get        {:handler GET-contact}}]]])
