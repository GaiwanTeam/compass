(ns co.gaiwan.compass.http
  (:require
   [co.gaiwan.compass.http.routes :as routes]
   [integrant.core :as ig]
   [reitit.ring :as ring]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.defaults :as ring-defaults]))

(defn router []
  (ring/router (routes/routing-table)))

(defn default-handler
  "The default fallback handler

  - Strip trailing slashes (will cause a redirect)
  - Handler 404/405/406 responses, see [[reitit.ring/create-default-handler]]
    for options
  "
  [opts]
  (ring/routes
   (ring/redirect-trailing-slash-handler {:method :strip})
   (ring/create-default-handler opts)))

(defn handler []
  (ring/ring-handler
   (router)
   (default-handler {})
   {:middleware [[ring-defaults/wrap-defaults ring-defaults/site-defaults]]}))

(defmethod ig/init-key :compass/http [_ {:keys [port]}]
  (jetty/run-jetty #((handler) %) {:port port
                                   :join? false}))

(defmethod ig/halt-key! :compass/http [_ http]
  (.stop http))
