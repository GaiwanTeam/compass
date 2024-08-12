(ns co.gaiwan.compass.routes.profiles
  "We need a page/route for user's profile"
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.profiles :as h]
   [ring.util.response :as response]
   [co.gaiwan.compass.util :as util]
   [io.pedestal.log :as log]
   [java-time.api :as time]))

(defn wrap-authentication [handler]
  (fn [req]
    (log/trace ::login-check (:identity req))
    (if-let [user (:identity req)]
      (handler req)
      {:status 401
       :headers {"Content-Type" "text/plain"}
       :body "Unauthorized"})))

(defn GET-profile [req]
  {:html/body [h/profile-detail
               (:identity req)]})

(defn GET-profile-form [req]
  {:html/body [h/profile-form
               (:identity req)]})

(defn params->profile-data
  [{:keys [name title user-id] :as params}]
  ;; (prn :params params)
  {:db/id (parse-long user-id)
   :user/name name
   :user/title title})

(defn POST-save-profile
  "Save profile to DB
  
  The typical params is like:
  {:name \"Arne\"
   :tityle \"CEO of Gaiwan\"
   :image {:content-type :filename :size :tempfile}}"
  [{:keys [params identity] :as req}]
  (let [{:keys [filename tempfile] :as image}  (:image params)
        file-id (str (:db/id identity))
        filepath (str util/upload-dir "/" file-id "_" filename)
        {:keys [tempids]} @(db/transact [(merge
                                          {:user/image-path filepath}
                                          (params->profile-data params))])]
    ;; (tap> req)
    ;; Copy the image file content to the uploads directory
    (io/copy tempfile (io/file filepath))
    (util/redirect ["/profiles"]
                   {:flash "Successfully Saved!"})))

(defn file-handler [req]
  (let [file (io/file util/upload-dir (get-in req [:path-params :filename]))]
    (if (.exists file)
      (response/file-response (.getPath file))
      (response/not-found "File not found"))))

(defn routes []
  [["/profiles"
    [""
     {:middleware [wrap-authentication]
      :get {:handler GET-profile}}]
    ["/edit"
     {:get {:handler GET-profile-form}}]
    ["/save"
     {:middleware [wrap-authentication]
      :post {:handler POST-save-profile}}]]
   ["/uploads/:filename"
    {:get {:handler file-handler}}]])
