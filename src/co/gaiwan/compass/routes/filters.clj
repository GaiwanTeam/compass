(ns co.gaiwan.compass.routes.filters
  "Filtering behavior"
  (:require
   [co.gaiwan.compass.html.filters :as filters]
   [co.gaiwan.compass.http.response :as redirect]
   [co.gaiwan.compass.model.session :as session]))

(defn GET-filters [req]
  {:html/layout false
   :html/body [filters/filter-section (:session-filters (:session req))]})

(defn PUT-filters [{:keys [params session identity] :as req}]
  (if (and (:my-activities params) (not identity))
    (redirect/requires-auth "/")
    {:hx/trigger  "filters-updated"
     :location "/"
     :session (assoc session
                     :session-filters
                     (merge session/default-filters
                            (update-vals params keyword)))}))

(defn routes []
  ["/filters"
   {:name :filters/index
    :put {:handler PUT-filters}
    :get {:handler GET-filters}}])

