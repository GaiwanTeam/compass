(ns co.gaiwan.compass.html.profiles
  "Views and components (hiccup/ornament) related to profiles"
  {:ornament/prefix "profiles-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [java-time.api :as time]
   [co.gaiwan.compass.html.sessions :as s]
   [lambdaisland.ornament :as o]))

(o/defprop --arc-thickness "30px")

(o/defstyled image-frame :div
  [:.img :w-full
   {:padding --arc-thickness
    #_#_:margin-left "-100%"}
   [:>* :w-full :aspect-square :rounded-full
    {:background-size "cover"
     :background-position "50% 50%"}]]
  ([{:profile/keys [image]} user]
   [:<>
    [:div.img
     [:div
      {:style {:background-image image}}]]]))

(o/defstyled edit-profile-btn :button
  ([user]
   [:<>
    {:hx-get "/profiles/edit"
     :hx-select "#form > *"
     :hx-target "#detail"}
    "Edit Profile"]))

(o/defstyled profile-detail :div#detail
  [image-frame :w-100px]
  ([{:discord/keys [access-token id refresh-token expires-at avatar-url]
     :user/keys [email handle name uuid title image-path] :as user}]
   [:<>
    [image-frame {:profile/image
                  (if-let [image (or image-path avatar-url)]
                    (str "url(" image ")")
                    (str "var(--gradient-" (inc (rand-int 7)) ")"))} user]
    [:div.details
     [:h3.title name]
     [:h3.subtitle title]]
    #_[:div (pr-str user)]
    [:div.actions
     [edit-profile-btn user]]]))

(o/defstyled profile-form :div#form
  [:form :grid {:grid-template-columns "10rem 1fr"} :gap-2]
  ([user]
   [:<>
    [:h2 "Edit Profile"]
    [:form {:method "POST" :action "/profiles/save" :enctype "multipart/form-data"}
     [:input {:type "hidden" :name "user-id" :value (:db/id user)}]
     [:div
      [:label {:for "name"} "Display Name"]
      [:input {:id "name" :name "name" :type "text"
               :required true :min-length 2}]]
     [:div
      [:label {:for "title"} "title"]
      [:input {:id "title" :name "title" :type "text"
               :min-length 2}]]
     [:div
      [:label {:for "image"} "Profile Image"]
      [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]]
     [:input {:type "submit" :value "Save"}]]]))
