(ns co.gaiwan.compass.http
  "System-level component: web server
  
  The config of http is in the file
  'resources/co/gaiwan/compass/system.edn' 
  
  Wiring the following components together
    - jetty  
      - ring handler
        - compass routes
        - compass default handler
        - compass middleware
  "
  (:require
   [co.gaiwan.compass.http.middleware :as middleware]
   [co.gaiwan.compass.http.routes :as routes]
   [integrant.core :as ig]
   [reitit.ring :as ring]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.defaults :as ring-defaults]
   [ring.middleware.session.cookie :as session-cookie]))

(defn router []
  (ring/router (routes/routing-table)))

(def ring-default-config
  {:params    {:urlencoded true
               :multipart  true
               :nested     true
               :keywordize true}
   :cookies   true
   :session   {:flash true
               :cookie-attrs {:http-only true}
               :store (session-cookie/cookie-store {:key "zsrpNuvjTqFcz6fg"})}
   :security  {:anti-forgery   true
               :frame-options  :sameorigin
               :content-type-options :nosniff}
   :static    {:resources "public"}
   :responses {:not-modified-responses true
               :absolute-redirects     false
               :content-types          true
               :default-charset        "utf-8"}})

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
   {:middleware [;; vvvvvvv  Request goes down
                 (fn [h]
                   (fn [r]
                     (assoc-in (h r) [:headers "Max-Age"] "0")))
                 [ring-defaults/wrap-defaults ring-default-config]
                 middleware/wrap-identity
                 middleware/wrap-render
                 #_handler
                 ;; ^^^^^^^  Response goes up
                 ]}))

(defmethod ig/init-key :compass/http [_ {:keys [port]}]
  (jetty/run-jetty #((handler) %) {:port port
                                   :join? false}))

(defmethod ig/halt-key! :compass/http [_ http]
  (.stop http))
