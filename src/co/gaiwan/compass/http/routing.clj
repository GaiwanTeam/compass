(ns co.gaiwan.compass.http.routing
  "Helpers and logic on top of reitit routing"
  (:require
   [integrant.repl.state :as state]
   [reitit.core :as reitit]))

(defn router []
  ((:compass/router integrant.repl.state/system)))

(defn url-for
  "Generate a URL based on route name + params. Will automatically differentiate
  path vs query params.

  Prefer this over hard-coding paths. You can `:refer [url-for]` in html
  namespaces for convenience.
  "
  ([route-name]
   (if (vector? route-name)
     (apply url-for route-name)
     (url-for route-name nil)))
  ([route-name params]
   (let [match       (reitit/match-by-name (router) route-name params)
         path-params (:path-params (reitit.impl/parse (:template match) {}))]
     (reitit/match->path match (apply dissoc params path-params)))))

