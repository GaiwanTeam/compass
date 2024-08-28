(ns co.gaiwan.compass.model.assets
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [hato.client :as hato])
  (:import
   (java.security MessageDigest)
   (java.util Formatter)))

(defn file-extension [mime]
  (let [[mime] (str/split mime #"\s*;\s*")]
    ({"image/png" "png"
      "image/jpeg" "jpeg"
      "image/gif" "gif"
      "image/webp" "webp"
      "image/svg+xml" "svg"} mime)))

(defn sha256 [bytes]
  (.digest (MessageDigest/getInstance "SHA-256") bytes))

(defn sha256-hex [bytes]
  (let [f (Formatter.)]
    (run! (fn [b]
            (.format ^Formatter f "%02x" (into-array Byte [b])))
          (sha256 bytes))
    (str f)))

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

(defn add-to-content-addressed-storage [content-type source]
  (let [ext      (file-extension content-type)
        bytes    (cond
                   (bytes? source)
                   source
                   (instance? java.io.File source)
                   (java.nio.file.Files/readAllBytes (.toPath source))
                   :else
                   (throw (ex-info {:source (class source)} "Unsupported data source")))
        filename (str (sha256-hex bytes) "." ext)
        target   (io/file (config/value :uploads/dir) filename)]
    (io/make-parents target)
    (when-not (.exists target)
      (with-open [f (io/output-stream target)]
        (.write f ^bytes bytes)))
    filename))

(defn download-image [url]
  (let [{:keys [^bytes body headers]} (hato/get url {:as :byte-array})]
    (add-to-content-addressed-storage (get headers "content-type") body)))

(comment
  (def r (hato/get "https://cdn.discordapp.com/avatars/758588684177768469/8b32119c1ae262544e2952ea60aaf9a7.png" {:as :byte-array}))

  (download-image  "https://cdn.discordapp.com/avatars/758588684177768469/8b32119c1ae262544e2952ea60aaf9a7.png"))
