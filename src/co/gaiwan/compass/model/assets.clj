(ns co.gaiwan.compass.model.assets
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]))

(defn asset-url [url]
  (if (str/starts-with? url "http")
    url
    (str (config/value :http/asset-path) "/" url)))
