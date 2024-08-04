(ns co.gaiwan.compass.html.layout
  (:require
   [ring.middleware.anti-forgery :as anti-forgery]
   [charred.api :as charred]
   [co.gaiwan.compass.html.navigation :as nav]
   [lambdaisland.ornament :as o]))

(o/defrules layout
  [:body
   {:max-width "100vw"}
   [:#app {:max-width "80rem" :margin "0 auto"}
    [:>main :p-2]]])

(defn base-layout [{:keys [head body flash user request] :as opts}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "/css/open-props.min.css"}]
    [:link {:rel "stylesheet" :href "/css/open-props-normalize.min.css"}]
    [:link {:rel "stylesheet" :href "/css/buttons.css"}]
    [:link {:rel "stylesheet" :href "/css/styles.css"}]
    [:script {:src "/js/htmx-1.9.12.js"}]
    [:script {:src "/js/cx.js"}]
    [:script {:src "/js/live.js#css"}]
    [:script {:src "/js/html-duration-picker.min.js"}]
    head]
   [:body {;; Have HTMX handle normal links
           :hx-boost true
           ;; Only replace what's in <main>, the navbar/menu don't get replaced
           :hx-select "main"
           :hx-target "main"

           :hx-disinherit "hx-target hx-select"
           ;; CSRF
           :hx-headers (charred/write-json-str {"x-csrf-token" anti-forgery/*anti-forgery-token*})}
    [:div#app
     [nav/menu-panel user]
     [:main
      (when flash
        [:p.flash flash])
      [nav/nav-bar user]
      body
      #_[:pre (with-out-str (clojure.pprint/pprint request))]]]]])
