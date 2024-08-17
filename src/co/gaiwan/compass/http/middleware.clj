(ns co.gaiwan.compass.http.middleware
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.layout :as layout]
   [co.gaiwan.compass.http.routing :as routing]
   [lambdaisland.hiccup :as hiccup]
   [reitit.ring :as ring]))

(swap! hiccup/kebab-prefixes conj "cx-")

(defn wrap-render
  "Handle rendering of hiccup for routes with backend rendering.

  Will look for `:html/body` in the response map, if it is there, then

  - it is rendered as hiccup
  - it is wrapped in a layout
  - the content-type is set to text/html
  - the status code defaults to 200

  Additionally :html/layout-fn and :html/head can be provided, either in the
  response map, or in (reitit) route data. Layout-fn must take two
  arguments, [head body], with elements that should be inserted in the head and
  body elements respectively.

  :html/head can be used to set the page title, social media tags, etc."
  [handler]
  (fn [req]
    (let [res           (handler req)
          route-data    (:data (ring/get-match req))
          head          (or (:html/head res)
                            (:html/head route-data))
          layout-fn     (first (filter some?
                                       [(:html/layout res)
                                        (:html/layout route-data)
                                        layout/base-layout]))
          body          (:html/body res)
          accept-header (get-in req [:headers "accept"])]
      ;; Render HTML if there's a `:html/body` key, and the client accepts
      ;; text/html, OR there is no `:body` key, because if there isn't then
      ;; there is nothing else to fall back to.
      (if (and body
               (or (not (:body res))
                   (and accept-header (some #{"text/html"} (str/split accept-header #",")))))
        (-> res
            (assoc :status (or (:status res) 200)
                   :body (hiccup/render (if layout-fn
                                          (layout-fn {:head head
                                                      :body body
                                                      :flash (:flash req)
                                                      :user (:identity req)
                                                      :request req})
                                          body)))
            (assoc-in [:headers "content-type"] "text/html; charset=utf-8"))
        res))))

(defn wrap-identity
  "Make `:identity` available in the request map when a user is logged in."
  [handler]
  (fn [req]
    (handler
     (if-let [uuid (get-in req [:session :identity])]
       (assoc req :identity
              (db/entity (db/q '[:find ?u .
                                 :in $ ?uid
                                 :where [?u :user/uuid ?uid]]
                               (db/db)
                               uuid)))
       req))))

(defn wrap-hx-responses
  "Recognize certain keys and use them to generate HTMX-specific responses.

  Currently handles:
  - :hx/trigger - emit a HX-Trigger header
  - :location - emit a HX-Redirect or Location header

  Will recognize if a request came from HTMX, and respond accordingly. This
  means that you can have both a :hx/trigger and a :location. In the case of a
  HTMX request the hx-trigger will be used, in the case of a regular HTTP
  request a redirect is returned.
  "
  [handler]
  (fn [req]
    (let [hx-request? (get-in req [:headers "hx-request"])]
      (let [resp (handler req)]
        (cond
          (and hx-request? (:hx/trigger resp))
          (-> resp
              (assoc
                :status (:status resp 200)
                :body (:body resp ""))
              (assoc-in [:headers "hx-trigger"] (:hx/trigger resp)))

          (:location resp)
          (-> resp
              (assoc
                :status (:status resp 302)
                :body (:body resp ""))
              (assoc-in [:headers (if hx-request? "hx-redirect" "location")] (routing/url-for (:location resp))))

          :else
          resp)))))
