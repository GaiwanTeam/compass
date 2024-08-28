(ns co.gaiwan.compass.model.assets
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]))

(defn image-url
  "Coerce to a valid image URL
  - blank/nil - show fallback image
  - absolute (or scheme-relative) - keep as is
  - relative - prefix with asset-path, which should match your web server config"
  [url]
  (cond
    (str/blank? url)
    (config/value :image/fallback)

    (or (str/starts-with? url "http")
        (str/starts-with? url "//"))
    url

    :else
    (str (config/value :http/asset-path) "/" url)))
