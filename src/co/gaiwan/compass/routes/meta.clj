(ns co.gaiwan.compass.routes.meta)

(defn routes []
  ["/health"
   {:name :index
    :get {:handler (fn [_] {:body "OK"})}}])
