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
  ([{:profile/keys [image] :as profile} user]
   [:<>
    [:div.img
     [:div
      {:style {:background-image image}}]]]))

(o/defstyled profile-detail :div
  [image-frame :w-100px]
  ([{:discord/keys [access-token id refresh-token expires-at]
     :user/keys [email handle name uuid image] :as profile}
    user]
   [:<>
    [image-frame {:profile/image
                  (if image
                    (str "url(" image ")")
                    (str "var(--gradient-" (inc (rand-int 7)) ")"))} user]
    [:div.details
     (pr-str profile)]]))
