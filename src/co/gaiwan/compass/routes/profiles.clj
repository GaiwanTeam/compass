(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clj.qrgen :as qr]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [io.pedestal.log :as log]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.db.queries :as q]
   [co.gaiwan.compass.html.profiles :as h]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.assets :as assets]
   [co.gaiwan.compass.model.attendees :as attendees]

   [ring.util.response :as ring-response]))

(defn GET-profile [{:keys [params] :as req}]
  ;; (log/debug :debug {:req req})
  {:html/body
   [h/profile-detail
    (if-let [profile-id (get-in req [:path-params :user-uuid])]
      (db/entity [:user/uuid (parse-uuid profile-id)])
      (:identity req)) (:identity req)]})

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
;; (params->profile-data params)
;; (parse-link-data params "public")

(defn parse-link-data [params variant]
  (map vector
       (get params (keyword (str variant "-link-type")))
       (get params (keyword (str variant "-link-ref")))))

(defn reconcile-links [user-id variant old-links new-links]
  (let [existing-pairs (map (juxt :profile-link/type :profile-link/href) old-links)
        del (remove (set new-links) existing-pairs)
        add (remove (set existing-pairs) new-links)]
    (concat
     (for [[k v] del
           :when (not (str/blank? v))
           :let [id (some #(when (= [k v] ((juxt :profile-link/type :profile-link/href) %))
                             (:db/id %)) old-links)]
           :when id]
       [:db/retractEntity id])
     (for [[k v] add
           :when (not (str/blank? v))]
       {(keyword (str variant "-profile/_links")) user-id
        :profile-link/type k
        :profile-link/href v}))))

(defn params->profile-data
  [{:keys [user-id hidden?
           private-name-switch
           bio_public name_public
           bio_private name_private
           rows-count
           image] :as params}]
  (let [user-id (parse-long user-id)
        user (db/entity user-id)
        public-links (reconcile-links
                      user-id
                      "public"
                      (:public-profile/links user)
                      (parse-link-data params "public"))
        private-links (reconcile-links
                       user-id
                       "private"
                       (:private-profile/links user)
                       (parse-link-data params "private"))]
    (cond-> (concat
             [{:db/id user-id
               :public-profile/bio bio_public
               :public-profile/name name_public
               :private-profile/bio bio_private}]
             public-links
             private-links)
      image
      (conj [:db/add user-id :public-profile/avatar-url
             (assets/add-to-content-addressed-storage (:content-type image) (:tempfile image))])

      (= "on" private-name-switch)
      (conj [:db/add user-id  :public-profile/hidden? true])

      (not= "on" private-name-switch)
      (conj [:db/retract user-id :public-profile/hidden? true])

      (and (str/blank? name_private) (:private-profile/name user))
      (conj [:db/retract user-id :private-profile/name (:private-profile/name user)])

      (not (str/blank? name_private))
      (conj [:db/add user-id :private-profile/name name_private]))))

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
   ["/user/:user-uuid"
    {:name :profile/show
     :middleware [[response/wrap-requires-auth]]
     :get        {:handler GET-profile}}]
   ["/me"
    {:name :profile/me
     :middleware [[response/wrap-requires-auth]]
     :get        {:handler GET-profile}}]])
