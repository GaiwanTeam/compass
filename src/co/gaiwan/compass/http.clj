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
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.http.middleware :as middleware]
   [co.gaiwan.compass.http.routes :as routes]
   [co.gaiwan.compass.util :as util]
   [integrant.core :as ig]
   [io.pedestal.log :as log]
   [reitit.ring :as ring]
   [reitit.ring.middleware.exception :as exception]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.defaults :as ring-defaults]
   [ring.middleware.session.cookie :as session-cookie]))

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

(defn exception-handler [error request]
  (let [error-id (random-uuid)]
    (log/error :http/error {:message "HTTP handler threw"
                            :error-id error-id
                            :request request}
               :exception error)
    {:status 500
     :html/body
     [:<>
      [:h1 (.getName (class error))]
      [:h2 (.getMessage error)]
      [:p "error-id=" error-id]
      (when (config/value :http/show-exception-details?)
        [:<>
         [:table {:style {:width "100%"}}
          [:tr
           [:td "ex-data"]
           [:td {:align "left"}
            [:pre {:style {:white-space "pre-wrap"}}
             (util/pprint-str (ex-data error))]]]
          [:tr
           [:td "Stack"]
           [:td {:align "left"}
            [:pre {:style {:white-space "pre-wrap"}}
             (for [l (.getStackTrace error)]
               (str l "\n"))]]]
          [:tr
           [:td "Request"]
           [:td {:align "left"}
            [:pre {:style {:white-space "pre-wrap"}}
             (util/pprint-str request)]]]]])]}))

(def ex-mw
  (exception/create-exception-middleware
   {::exception/default exception-handler}))

(defn router []
  (ring/router
   (routes/routing-table)
   {:data {:middleware [ex-mw]}}))

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
  (log/info :http/starting {:port port})
  (jetty/run-jetty #((handler) %) {:port port
                                   :join? false}))

(defmethod ig/halt-key! :compass/http [_ http]
  (.stop http))
