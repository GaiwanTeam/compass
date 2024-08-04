(ns co.gaiwan.compass.routes.filters
  "Filtering behavior"
  (:require
   [co.gaiwan.compass.html.filters :as filters]))

(def defaults
  {:include-past false})

(defn GET-filters [req]
  {:html/layout false
   :html/body [filters/filter-section (:session-filters (:session req))]})

(defn PUT-filters [{:keys [params session] :as req}]
  (tap> req)
  {:status 200
   :session (assoc session
                   :session-filters
                   (merge defaults
                          (update-vals params keyword)))
   :headers {"HX-Trigger" "filters-updated"}
   })

(defn routes []
  ["/filters" {:put {:handler #'PUT-filters}
               :get {:handler #'GET-filters}}])

