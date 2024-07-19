(ns co.gaiwan.compass.routes.meta)

(defn routes []
  ["/health"
   {:name :health
    :get {:handler (fn [_] {:body "OK"})}}])
