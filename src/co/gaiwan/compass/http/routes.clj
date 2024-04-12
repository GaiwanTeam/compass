(ns co.gaiwan.compass.http.routes)

(defn routing-table []
  ["/"
   {:name :index
    :get {:handler (fn [_] {:status 200 :body "ok"})}}])
