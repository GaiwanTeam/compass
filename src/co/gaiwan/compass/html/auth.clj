(ns co.gaiwan.compass.html.auth
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defstyled popup :div
  :flex-col :items-center
  :gap-4
  [:.top :flex :self-end :p-2]
  [:p :m-4]
  [:.discord-login :mb-4 :py-3 :px-4
   {:background-color "#7289da"
    :border-radius "0.5rem"
    :color t/--gray-0
    :text-align "center"}
   [:&:hover {:text-decoration "none"}]]
  [graphics/cross {:width "3rem" :height "2.25rem" :padding "0.4rem"}]
  [graphics/discord {:height "2rem" :width "2rem" :--_logo-color t/--gray-0}]
  ([next-url]
   [:<>
    [:div.top
     [graphics/cross {:class "btn close-button" :on-click "window.modal.close()"}]]
    [:p "Please authenticate using Discord to make full use of the Compass app. This will also give you access to our Discord server where you can chat with speakers and attendees."]
    [:a.btn.discord-login
     {:href (oauth/flow-init-url (if next-url
                                   {:redirect-url next-url}
                                   {}))}
     [graphics/discord]
     "Login with discord"]]))
