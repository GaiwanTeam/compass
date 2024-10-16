(ns co.gaiwan.compass.html.auth
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defstyled discord-button :a.btn
  :mb-4 :py-3 :px-4
  {:background-color "#7289da"
   :border-radius t/--radius-2
   :color t/--gray-0
   :text-align "center"
   :--_ink-shadow "none"}
  [:&:hover {:text-decoration "none"}]
  [graphics/discord {:height "2rem" :width "2rem" :--_logo-color t/--gray-0}]
  ([{:keys [next-url text]}]
   [:<>
    {:hx-boost "false"
     :href (if next-url
             (str "/oauth2/discord/redirect?redirect_url=" next-url)
             (str "/oauth2/discord/redirect"))}
    [graphics/discord]
    (or text "Continue with Discord")]))

(o/defstyled popup :div
  :flex-col :items-center
  :gap-4
  [:.top :flex :self-end :p-2]
  [:p :m-4 :mt-0]
  [:.discord-login]
  [graphics/cross {:width "3rem" :height "2.25rem" :padding "0.4rem"
                   :--_icon-color t/--text-1}]
  ([next-url]
   [:<>
    [:a.top
     [graphics/cross {:class "btn close-button" :on-click "window.modal.close()"}]]
    [:p "You can authenticate using Discord to make full use of the Compass app. This will also give you access to our Discord server where you can chat with speakers and attendees."]
    [discord-button {:next-url next-url}]]))
