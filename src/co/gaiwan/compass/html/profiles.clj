(ns co.gaiwan.compass.html.profiles
  "Views and components (hiccup/ornament) related to profiles"
  {:ornament/prefix "profiles-"}
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [java-time.api :as time]
   [co.gaiwan.compass.html.sessions :as s]
   [lambdaisland.ornament :as o]
   [markdown-to-hiccup.core :as m]))

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
    {:hx-get "/profile/edit"
     :hx-select "#form > *"
     :hx-target "#detail"}
    "Edit Profile"]))

(o/defstyled profile-detail :div#detail
  [image-frame :w-100px]
  ([{:public-profile/keys [name avatar-url]
     :user/keys [uuid] :as user}]
   [:<>
    [image-frame {:profile/image
                  (if-let [image (or (:public-profile/avatar-url user) avatar-url)]
                    (str "url(" image ")")
                    (str "var(--gradient-" (inc (rand-int 7)) ")"))} user]
    [:div.details
     [:h3.title name]]
    #_[:div (pr-str user)]
    [:div.actions
     [edit-profile-btn user]]]))

(o/defstyled attendee-card :div
  [image-frame :w-100px]
  ([{:discord/keys [avatar-url]
     :public-profile/keys [name bio]
     :user/keys [uuid] :as user}]
   [:<>
    [image-frame {:profile/image
                  (if-let [image (or (:public-profile/avatar-url user) avatar-url)]
                    (str "url(" image ")")
                    (str "var(--gradient-" (inc (rand-int 7)) ")"))} user]
    [:div.details
     [:h3 name]
     (when bio
       [:textarea (m/md->hiccup bio)])]]))

(o/defstyled private-name :div
  ([user {:keys [private-name-switch] :as params}]
   (tap> {:params params})
   (if (= "on" private-name-switch)
     [:div#private-name-block
      [:label {:for "private-name"} "Confidential Name"]
      [:input {:id "private-name" :name "name_private" :type "text"
               :required true :min-length 2
               :value (:private-profile/name user)}]]
     [:div#private-name-block])))

(o/defstyled profile-form :div#form
  [:form :grid {:grid-template-columns "10rem 1fr"} :gap-2]
  ([user]
   [:<>
    [:h2 "Edit Profile"]
    [:form {:method "POST" :action "/profile/save" :enctype "multipart/form-data"}
     [:input {:type "hidden" :name "user-id" :value (:db/id user)}]
     [:label {:for "hidding"}
      [:input {:id "hidding" :name "hidden?" :type "checkbox"
               :checked (:public-profile/hidden? user)}]
      "Hide profile from public listings?"]
     [:div
      [:label {:for "name"} "Name (public)"]
      [:input {:id "name" :name "name_public" :type "text"
               :required true :min-length 2
               :value (:public-profile/name user)}]]
     [:div
      [:label {:for "show-another-name"}
       [:input {:id "show-another-name" :name "private-name-switch" :type "checkbox"
                :hx-get (url-for :profile/private-name)
                :hx-target "#private-name-block"
                :hx-select "#private-name-block"
                :hx-trigger "change"
                :hx-swap "outerHTML"}]
       "Show different name to confidantes?"]
      [:div {:id "private-name-block"}]]
     [:div
      [:label {:for "image"} "Avatar"]
      [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]]

     [:div
      [:label {:for "bio_public"}
       "Bio (public, markdown)"
       [:input {:id "bio_public" :name "bio_public" :type "text"
                :value (:public-profile/bio user)}]]]

     [:div
      [:label {:for "bio_private"}
       "Bio (confidential, markdown)"
       [:input {:id "bio_private" :name "bio_private" :type "text"
                :value (:private-profile/bio user)}]]]

     [:input {:type "submit" :value "Save"}]]]))
