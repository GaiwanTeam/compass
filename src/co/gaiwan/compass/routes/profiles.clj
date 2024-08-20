(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clojure.java.io :as io]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.profiles :as h]
   [co.gaiwan.compass.http.response :as response]
   [ring.util.response :as ring-response]))

(defn GET-profile [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn GET-profile-form [req]
  {:html/body [h/profile-form
               (:identity req)]})

(defn params->profile-data
  [{:keys [name user-id] :as params}]
  {:db/id (parse-long user-id)
   :public-profile/name name})

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
        {:keys [tempids]} @(db/transact [(merge
                                          {:public-profile/avatar-url (str "/" filepath)}
                                          (params->profile-data params))])]
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

(defn GET-attendees [req])

(defn routes []
  [["/profile"
    [""
     {:middleware [[response/wrap-requires-auth]]
      :get        {:handler GET-profile}}]
    ["/edit"
     {:get {:handler GET-profile-form}}]
    ["/save"
     {:middleware [[response/wrap-requires-auth]]
      :post       {:handler POST-save-profile}}]]
   ["/uploads/:filename"
    {:middleware [[response/wrap-requires-auth]]
     :get        {:handler file-handler}}]
   ["/attendees"
    {:middleware [[response/wrap-requires-auth]]
     :get        {:handler GET-attendees}}]])
