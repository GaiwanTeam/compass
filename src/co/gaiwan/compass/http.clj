(ns co.gaiwan.compass.http
  (:require
   [co.gaiwan.compass.http.routes :as routes]
   [reitit.ring :as ring]
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty]))

(defn build-router []
  (ring/router (routes/routing-table)))

(defn build-handler []
  (ring/ring-handler
   (build-router)
   #_default-handler
   ))

(defmethod ig/init-key :compass/http [_ {:keys [port]}]
  (jetty/run-jetty (build-handler) {:port port
                                    :join? false}))

(defmethod ig/halt-key! :compass/http [_ http]
  (.stop http))
