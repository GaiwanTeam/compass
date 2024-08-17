(ns co.gaiwan.compass.http.response
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.html.auth :as auth-html]
   [ring.util.response :as response]))

(defn redirect
  "Returns a Ring response for an HTTP 302 redirect. Status may be
  a key in redirect-status-codes or a numeric code. Defaults to 302"
  ([url]
   (redirect url nil))
  ([url {:keys [status flash push-url?]
         :or {status :found}}]
   (let [url (str (if (vector? url)
                    (str/join "/" url)
                    url))]
     (cond-> {:status  (response/redirect-status-codes status status)
              :headers {"Location" url
                        "HX-Redirect" url}
              :body    ""}
       flash
       (assoc :flash flash)))))

(defn requires-auth
  "Ring response that instructs HTMX to render the login dialog"
  [next-url]
  {:html/layout false
   :html/body [auth-html/popup next-url]
   :headers {"HX-Retarget" "#modal"
             "HX-Reswap" "innerHTML"
             "HX-Reselect" (str "." auth-html/popup)}})

(defn wrap-requires-auth
  "Middleware that shows the login dialog for non-GET requests if the user is not
  logged in"
  [handler]
  (fn [req]
    (tap> req)
    (if (not (:identity req))
      (if (get-in req [:headers "hx-request"])
        (requires-auth (:uri req))
        (redirect (str "/?login")))
      (handler req))))
