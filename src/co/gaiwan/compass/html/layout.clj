(ns co.gaiwan.compass.html.layout)

(defn layout [body {:keys [title] :as opts}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title title]
    [:link {:rel "stylesheet" :href "/css/open-props-1.7.4.min.css"}]
    [:link {:rel "stylesheet" :href "/css/open-props-normalize-1.7.4.min.css"}]
    [:link {:rel "stylesheet" :href "/css/styles.css"}]
    [:script {:src "/js/htmx-1.9.12.js"}]
    [:script {:src "/js/live.js#css"}]
    ]

   [:body
    body]])
