(ns co.gaiwan.compass.html.profiles
  "Views and components (hiccup/ornament) related to profiles"
  {:ornament/prefix "profiles-"}
  (:require
   [co.gaiwan.compass.css.tokens :as t :refer :all]
   [co.gaiwan.compass.db.queries :as queries]
   [co.gaiwan.compass.html.components :as c]
   [co.gaiwan.compass.http.routing :refer [url-for]]
   [co.gaiwan.compass.model.user :as user]
   [lambdaisland.ornament :as o]))

;; UI of profile detail

(o/defstyled edit-profile-btn :a.btn
  ([user]
   [:<>
    {:href (url-for :profile/edit)} "Edit Profile"]))

(o/defstyled profile-detail :div#detail
  [c/image-frame :w-100px {t/--arc-thickness "7%"}]
  ([{:public-profile/keys [name hidden?]
     :user/keys [uuid] :as user}]
   [:<>
    [:div [c/image-frame {:profile/image (user/avatar-css-value user)}]]
    [:div.details
     [:h3.title name]]
    (if hidden?
      [:label "Hide profile from public listing"]
      [:label "Show profile from public listing"])
    (when (:private-profile/name user)
      [:div
       [:label "Another Name:"]
       [:label (:private-profile/name user)]])

    #_[:div (pr-str user)]
    ;; Disable Edit Profile before we can show profile details pretty
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
  ([{:keys [variant] :as link}]
   [:<>
    [:td
     (when (:db/id link)
       [:input {:type "hidden" :name (str variant "-link-id[]") :value (:db/id link)}])
     (let [link-type (:profile-link/type link)]
       [:select {:name (str variant "-link-type[]")}
        [:option {:value "email" :selected (= link-type "email")} "Email"]
        [:option {:value "mastodon" :selected (= link-type "mastodon")} "Mastodon"]
        [:option {:value "linkedin" :selected (= link-type "linkedin")} "LinkedIn"]
        [:option {:value "personal-site" :selected (= link-type "personal-site")} "Personal Site"]
        [:option {:value "other" :selected (= link-type "other")} "Other"]])]
    [:td
     [:input
      {:type "text"
       :name (str variant "-link-ref[]")
       :value (str (:profile-link/href link))}]]]))

(def always-show ["email" "mastodon"])

(o/defstyled links-table :div
  ([links {:keys [caption variant]}]
   (let [link-map (into {} (map (juxt :profile-link/type :profile-link/href)) links)
         link-vals (concat
                    (for [t always-show]
                      [t (get link-map t)])
                    (apply dissoc link-map always-show))]
     [:<>
      [:template
       [row {:profile-link/type "other"
             :profile-link/href ""
             :variant variant}]]
      [:table
       [:thead
        [:tr
         [:th {:colspan 2} caption]]]
       [:tbody
        (for [[t h] link-vals]
          [row {:profile-link/type t
                :profile-link/href h
                :variant variant}])]]
      [:input#add-link
       {:value "+ Add Link"
        :type "button"
        :on-click "let form = this.parentElement; form.querySelector('tbody').append(form.querySelector('template').content.cloneNode(true))"}]])))

(o/defstyled profile-form :div#form
  [c/image-frame :w-100px {t/--arc-thickness "7%"}]
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
  [:.contact-card
   :shadow-3
   :my-6
   {:background-color t/--surface-2
    :padding t/--size-3
    :border-radius t/--size-2}
   [#{:textarea "input[type='text']"} {:background-color t/--surface-3}]]
  ([user]
   (def user user)
   [:<>
    [:h2 "Edit Profile"]
    [:form {:method "POST" :action "/profile/save" :enctype "multipart/form-data"}
     [:input {:type "hidden" :name "user-id" :value (:db/id user)}]
     [:label {:for "hidding"}
      [:input {:id "hidding" :name "hidden?" :type "checkbox"
               :checked (:public-profile/hidden? user)}]
      "Hide profile from public listings?"]
     [:label {:for "name"} "Display Name"]
     [:input {:id "name" :name "name_public" :type "text"
              :required true
              :value (:public-profile/name user)}]
     [:div
      [:label {:for "bio_public"}
       "Bio (accepts markdown)"]
      [:textarea {:id "bio_public" :name "bio_public"}
       (when (:public-profile/bio user)
         (:public-profile/bio user))]]
     [:div
      (when user
        [c/image-frame {:profile/image (user/avatar-css-value user)}])
      [:label {:for "image"} "Avatar"]
      [:input {:id "image" :name "image" :type "file" :accept "image/png, image/jpeg"}]]
     [links-table (:public-profile/links user)
      {:variant "public"
       :caption "Public Profile Links"}]

     [:div.contact-card
      [:h3 "Contact Card"]
      [:p.info "This information is only shown to people you add as a contact."]
      [:label {:for "name"} "Name"]
      [:input {:id "name" :name "name_private" :type "text"
               :value (:private-profile/name user)}]
      [:div
       [:label {:for "bio_private"}
        "Private Bio (accepts markdown)"]
       [:textarea {:id "bio_private" :name "bio_private"}
        (when (:private-profile/bio user)
          (:private-profile/bio user))]]
      [links-table (:private-profile/links user)
       {:variant "private"
        :caption "Links Visible to Contacts"}]
      ]

     [:input {:type "submit" :value "Save Profile"}]]
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
