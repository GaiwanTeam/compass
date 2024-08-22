(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clojure.java.io :as io]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.db.queries :as q]
   [co.gaiwan.compass.model.attendees :as attendees]
   [co.gaiwan.compass.html.profiles :as h]
   [co.gaiwan.compass.http.response :as response]
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
        href-key (keyword (str "link-ref-" idx))
        type-key (keyword (str "link-type-" idx))
        private-link-key (keyword (str "private-" idx))
        public-link-key (keyword (str "public-" idx))
        href-val (href-key params)
        type-val (type-key params)
        private-link-val (private-link-key params)
        public-link-val (public-link-key params)
        profile-data {:db/id (str "temp-" idx)
                      :profile-link/user user-id
                      :profile-link/type type-val
                      :profile-link/href href-val}]
    (cond-> [profile-data]
      private-link-val
      (conj [:db/add user-id :private-profile/links (str "temp-" idx)])
      public-link-val
      (conj [:db/add user-id :public-profile/links (str "temp-" idx)]))))

(defn params->profile-data
  [{:keys [user-id hidden?
           private-name-switch
           bio_public name_public
           bio_private name_private
           rows-count] :as params}]
  (tap> params)
  (let [user-id (parse-long user-id)
        out (cond-> {:db/id user-id
                     :public-profile/bio bio_public
                     :private-profile/bio bio_private
                     :public-profile/name name_public}
              hidden?
              (assoc :public-profile/hidden? true))
        ;; handle the links data
        links  (vec (mapcat #(index->link-data params %)
                            (range (parse-long rows-count))))
        txes (conj links out)
        ;; handle the user private name
        txes (cond
               (and name_private
                    (= "on" private-name-switch))
               (conj txes [:db/add user-id :private-profile/name name_private])
               (and name_private
                    (nil? private-name-switch))
               (conj txes [:db/retract user-id :private-profile/name  name_private])
               :else
               txes)]
    txes))

(defn POST-save-profile
  "Save profile to DB

  The typical params is like:
  {:name \"Arne\"
   :tityle \"CEO of Gaiwan\"
   :image {:content-type :filename :size :tempfile}}"
  [{:keys [params identity] :as req}]
  (let [{:keys [filename tempfile] :as image}  (:image params)
        file-id (str (:db/id identity))
        filepath (str (config/value :uploads/dir) "/" file-id "_" filename)
        ;; creating the transaction
        txes (conj
              (params->profile-data params)
              {:db/id (parse-long (:user-id params))
               :public-profile/avatar-url (str "/" filepath)})
        _ (tap> {:txes txes})
        {:keys [tempids]} @(db/transact txes)]
    ;; (tap> req)
    ;; Copy the image file content to the uploads directory
    (io/copy tempfile (io/file filepath))
    (response/redirect "/profile"
                       {:flash "Successfully Saved!"})))

(defn file-handler [req]
  (let [file (io/file (config/value :uploads/dir) (get-in req [:path-params :filename]))]
    (if (.exists file)
      (ring-response/file-response (.getPath file))
      (ring-response/not-found "File not found"))))

(defn GET-attendees [req]
  (let [attendees (q/all-users)]
    {:html/body
     [:<>
      [:p "The Attendees List"]
      (for [atd (attendees/user-list attendees)]
        (h/attendee-card atd))]}))

(defn routes []
  [["/profile"
    [""
     {:middleware [[response/wrap-requires-auth]]
      :get        {:handler GET-profile}}]
    ["/edit"
     {:get {:handler GET-profile-form}}]
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
   ["/attendees"
    {:middleware [[response/wrap-requires-auth]]
     :get        {:handler GET-attendees}}]])
