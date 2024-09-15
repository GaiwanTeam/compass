(ns co.gaiwan.compass.model.assets
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [co.gaiwan.compass.images :as images]
   [hato.client :as hato])
  (:import
   (java.security MessageDigest)
   (java.util Formatter)
   (javax.imageio ImageIO)))

(set! *warn-on-reflection* true)

(defn file-extension [mime]
  (let [[mime] (str/split mime #"\s*;\s*")]
    ({"image/png" "png"
      "image/jpeg" "jpeg"
      "image/gif" "gif"
      "image/webp" "webp"
      "image/svg+xml" "svg"} mime)))

(defn ext->mime [path]
  (let [ext (str/lower-case (last (str/split (str path) #"\.")))]
    ({"png" "image/png"
      "jpeg" "image/jpeg"
      "gif" "image/gif"
      "webp" "image/webp"
      "svg" "image/svg+xml"} ext)))

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
                   (java.nio.file.Files/readAllBytes (.toPath ^java.io.File source))
                   (instance? java.awt.image.BufferedImage source)
                   (images/image-png-bytes source)
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

(defn jpeg-orientation [f]
  ;; 1: Normal (0° rotation)
  ;; 3: Upside-down (180° rotation)
  ;; 6: Rotated 90° counterclockwise (270° clockwise)
  ;; 8: Rotated 90° clockwise (270° counterclockwise)
  (case
      (.getInt
       (.getFirstDirectoryOfType
        (com.drew.imaging.ImageMetadataReader/readMetadata (io/file f))
        com.drew.metadata.exif.ExifIFD0Directory)
       com.drew.metadata.exif.ExifIFD0Directory/TAG_ORIENTATION)
    3 :upside-down
    6 :counterclockwise
    8 :clockwise))

(defn resize-image
  ([path max-width max-height]
   (resize-image (ext->mime path) path max-width max-height))
  ([mime-type path max-width max-height]
   (let [source (images/read-img path)
         width  (images/width source)
         height (images/height source)
         aspect (/ width height)
         scale (min (/ max-width width)
                    (/ max-height height))
         rotation (when (= "image/jpeg" mime-type)
                    (jpeg-orientation (io/file path)))
         img (images/scale-img source scale)]
     (case rotation
       nil img
       :upside-down (images/rotate-180 img)
       :counterclockwise (images/rotate-clockwise img)
       :clockwise (images/rotate-counterclockwise img)))))

(comment
  (def path "/home/arne/Downloads/IMG_20240914_212508.jpg")
  (images/write-png "/tmp/resized.png"
                    (resize-image path 500 500))
  (add-to-content-addressed-storage "image/png" (resize-image path 500 500))
  (def r (hato/get "https://cdn.discordapp.com/avatars/758588684177768469/8b32119c1ae262544e2952ea60aaf9a7.png" {:as :byte-array}))

  (download-image  "https://cdn.discordapp.com/avatars/758588684177768469/8b32119c1ae262544e2952ea60aaf9a7.png"))
