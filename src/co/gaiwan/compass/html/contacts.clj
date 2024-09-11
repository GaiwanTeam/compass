(ns co.gaiwan.compass.html.contacts
  "Views and components (hiccup/ornament) related to contacts"
  {:ornament/prefix "contacts-"}
  (:require
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.html.components :as c]
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.user :as user]
   [lambdaisland.ornament :as o]
   [markdown-to-hiccup.core :as m]))

(o/defstyled qr-dialog :div
  :m-4
  ([]
   [:<>
    [:h2 "Add Contact"]
    [:img {:src (url-for :contact/qr-png)}]]))

;; UI of attendee list

(o/defstyled attendee-card :div
  [c/image-frame :w-100px]
  ([{:public-profile/keys [name hidden? bio]
     :user/keys [uuid] :as user}]
   [:<>
    [c/image-frame {:profile/image (user/avatar-css-value user)}]
    [:div.details
     [:h3 name]
     (if hidden?
       [:label "Hide profile from public listing"]
       [:label "Show profile from public listing"])
     (when (:private-profile/name user)
       [:div
        [:label "Another Name:"]
        [:label (:private-profile/name user)]])
     (when bio
       [:textarea (m/md->hiccup bio)])]]))

(o/defstyled contact-detail :div
  [:.heading :flex :justify-between
   :mb-3]
  [:.contact-list :w-full :ga-4]
  [:.remove-btn :cursor-pointer :border-none {:background-color t/--surface-3}]
  [:.remove-btn
   :font-semibold
   {:color t/--text-1}
   [#{:&:hover :&:active}
    {:background-color t/--surface-4}]]
  [:.contact :flex :items-center :my-2 :py-2
   :shadow-2 :font-size-3
   {:background-color t/--surface-2}
   [:.details :flex-grow :mr-2]
   [c/image-frame :w-50px {--arc-thickness "7%"} :mx-2]]
  [:.profile-name :font-semibold]
  [:.email :font-size-3 {:color t/--text-2}]

  ([{:public-profile/keys [name]
     :user/keys [uuid] :as user}]
   [:<>
    [:div.heading
     [:h2 "Your Contacts"]
     [:button {:hx-target "#modal"
               :hx-get (url-for :contact/qr)}
      [graphics/scan-icon] "Add Contact"]]
    [:div
     [:a
      {:href (url-for :contacts/index)
       :style {:display "none"}
       :hx-trigger "contact-deleted from:body"}]
     [:div.contact-list
      (for [c (:user/contacts user)]
        [:div.contact
         [c/image-frame {:profile/image (user/avatar-css-value c)}]
         [:div.details
          [:div.profile-name (:public-profile/name c)]
          [:div.email (:discord/email c)]]
         [:button.remove-btn {:hx-delete (url-for :contact/link {:id (:db/id c)})}
          [graphics/person-remove] "Remove"]])]]]))
