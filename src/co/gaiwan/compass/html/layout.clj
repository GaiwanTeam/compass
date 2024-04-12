(ns co.gaiwan.compass.html.layout)

(defn layout [body {:keys [title] :as opts}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title title]
    [:link {:rel "stylesheet" :href "styles.css"}]]
   [:body
    body]])
