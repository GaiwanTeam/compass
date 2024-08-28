(ns co.gaiwan.compass.html.profiles
  "Views and components (hiccup/ornament) related to profiles"
  {:ornament/prefix "profiles-"}
  (:require
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.db.queries :as queries]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.user :as user]
   [lambdaisland.ornament :as o]
   [markdown-to-hiccup.core :as m]))

(o/defprop --arc-thickness "30px")

(o/defstyled image-frame :div
  [:.img :w-100px
   {#_#_:padding --arc-thickness
    #_#_:margin-left "-100%"}
   [:>* :w-full :aspect-square :rounded-full
    {:background-size "cover"
     :background-position "50% 50%"}]]
  ([{:profile/keys [image]}]
   [:<>
    [:div.img
     [:div
      {:style {:background-image image}}]]]))

;; UI of attendee list

(o/defstyled attendee-card :div
  [image-frame :w-100px]
  ([{:public-profile/keys [name bio]
     :user/keys [uuid] :as user}]
   [:<>
    [image-frame {:profile/image (user/avatar-css-value user)}]
    [:div.details
     [:h3 name]
     (when bio
       [:textarea (m/md->hiccup bio)])]]))

;; UI of profile detail

(o/defstyled edit-profile-btn :button
  ([user]
   [:<>
    {:hx-get "/profile/edit"
     :hx-select "#form"
     :hx-target "#detail"
     :hx-swap "outerHTML"}
    "Edit Profile"]))

(o/defstyled profile-detail :div#detail
  [image-frame :w-100px]
  ([{:public-profile/keys [name]
     :user/keys [uuid] :as user}]
   [:<>
    [image-frame {:profile/image (user/avatar-css-value user)}]
    [:div.details
     [:h3.title name]]
    #_[:div (pr-str user)]
    [:div.actions
     [edit-profile-btn user]]]))

(o/defstyled private-name :div
  ([user {:keys [private-name-switch] :as params}]
   (if (= "on" private-name-switch)
     [:div#private-name-block
      [:label {:for "private-name"} "Confidential Name"]
      [:input {:id "private-name" :name "name_private" :type "text"
               :required true :min-length 2
               :value (:private-profile/name user)}]]
     [:div#private-name-block])))

(o/defstyled row :tr.link-row
  ([link {:keys [row-index] :as params}]
   [:<>
    [:td
     ;; (pr-str link)
     (when (:db/id link)
       [:input {:type "hidden" :name (str "link-id-" row-index) :value (:db/id link)}])
     (let [link-type (:profile-link/type link)]
       [:select {:name (str "link-type-" row-index)}
        [:option {:value "email" :selected (= link-type "email")} "Email"]
        [:option {:value "twitter" :selected (= link-type "twitter")} "Twitter"]
        [:option {:value "mastodon" :selected (= link-type "mastodon")} "Mastodon"]
        [:option {:value "linkedin" :selected (= link-type "linkedin")} "LinkedIn"]
        [:option {:value "personal-site" :selected (= link-type "personal-site")} "Personal Site"]
        [:option {:value "other" :selected (= link-type "other")} "Other"]])]
    [:td
     [:input (cond-> {:name (str "link-ref-" row-index) :type "text" :required true
                      :min-length 2}
               (:db/id link)
               (assoc :value (:profile-link/href link)))]]
    [:td
     [:input {:name (str "public-" row-index) :type "checkbox"
              :checked (:public-link link)}]]
    [:td
     [:input {:name (str "private-" row-index) :type "checkbox"
              :checked (:private-link link)}]]]))

(o/defstyled links-table :div
  ([link {:keys [row-index] :as params}]
   [:table
    [row link params]]))

(o/defstyled profile-form :div#form
  [#{:label :input} :block]
  [:label
   :mb-1 :mt-2
   {:font-size t/--font-size-3
    :font-weight t/--font-weight-6}]
  [#{:input :textarea :select} ["&:not([type=checkbox])" :w-full :mb-3]]
  [:label
   :justify-start
   :items-center
   ["&:has([type=checkbox])"
    :flex
    :gap-3]]
  [:table :w-full]
  ([user]
   [:<>
    [:h2 "Edit Profile"]
    [:form {:method "POST" :action "/profile/save" :enctype "multipart/form-data"}
     [:input {:type "hidden" :name "user-id" :value (:db/id user)}]
     [:label {:for "hidding"}
      [:input {:id "hidding" :name "hidden?" :type "checkbox"
               :checked (:public-profile/hidden? user)}]
      "Hide profile from public listings?"]
     [:label {:for "name"} "Name (public)"]
     [:input {:id "name" :name "name_public" :type "text"
              :required true :min-length 2
              :value (:public-profile/name user)}]
     [:label {:for "show-another-name"}
      [:input {:id "show-another-name" :name "private-name-switch" :type "checkbox"
               :hx-get (url-for :profile/private-name)
               :hx-target "#private-name-block"
               :hx-select "#private-name-block"
               :hx-trigger "change"
               :hx-swap "outerHTML"}]
      "Show different name to confidantes?"]
     [:div.input-block {:id "private-name-block"}]
     [:div
      (when user
        [image-frame {:profile/image (user/avatar-css-value user)}])
      [:label {:for "image"} "Avatar"]
      [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]]
     [:div
      [:label {:for "bio_public"}
       "Bio (public, markdown)"]
      [:textarea {:id "bio_public" :name "bio_public"}
       (when (:public-profile/bio user)
         (:public-profile/bio user))]]

     [:div
      [:label {:for "bio_private"}
       "Bio (confidential, markdown)"]
      [:textarea {:id "bio_private" :name "bio_private"}
       (when (:private-profile/bio user)
         (:private-profile/bio user))]]
     [:div
      [:table
       [:thead
        [:tr
         [:th {:colspan 2} "Links"]
         [:th "public"]
         [:th "confidential"]]]
       [:tbody#links-block
        (let [links (map (fn [link]
                           (cond-> link
                             (:public-profile/_links link)
                             (assoc :public-link true)
                             (:private-profile/_links link)
                             (assoc :private-link true)))
                         (queries/all-links (:db/id user)))]
          (map-indexed
           (fn [idx itm]
             [row itm {:row-index idx}]) links))]]
      [:input#rows-count {:type "hidden" :name "rows-count" :value (count (queries/all-links (:db/id user)))}]
      [:input#add-link {:type "button" :value "Add Links"
                        :hx-get (url-for :profile/add-link)
                        :hx-target "#links-block"
                        :hx-select ".link-row"
                        :hx-trigger "click"
                        :hx-swap "beforeend"}]]
     [:input {:type "submit" :value "Save"}]]
    [:script
     "document.getElementById('add-link').addEventListener('htmx:configRequest', function(evt) {
      const url = new URL(evt.detail.path, window.location.origin);
      var elements = document.querySelectorAll('tr.link-row');
      url.searchParams.set('row-index', elements.length);
      // update hidden field
      document.getElementById('rows-count').setAttribute('value', elements.length+1);
      // update URL
      evt.detail.path = url.toString();
     });"]]))
