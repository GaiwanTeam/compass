(ns co.gaiwan.compass.html.auth
  (:require
   [co.gaiwan.compass.html.graphics :as graphics]
   [co.gaiwan.compass.http.oauth :as oauth]
   [lambdaisland.ornament :as o]))

(o/defstyled popup :div
  ([next-url]
   [:<>
    [graphics/cross {:on-click "window.modal.close()"}]
    [:p "Please authenticate using Discord to make full use of the Compass app. This will also give you access to our Discord server where you can chat with speakers and attendees."]
    [:a {:href (oauth/flow-init-url (if next-url
                                      {:redirect-url next-url}
                                      {}))} "Login with discord"]]))
