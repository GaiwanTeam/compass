(ns co.gaiwan.compass.images
  (:require
   [clojure.java.io :as io])
  (:import
   (java.awt.geom AffineTransform)
   (java.awt.image AffineTransformOp BufferedImage DataBufferByte Raster)
   (java.io ByteArrayInputStream ByteArrayOutputStream)
   (java.lang ArrayIndexOutOfBoundsException)
   (javax.imageio ImageIO)))

(set! *warn-on-reflection* true)

(def ^"[F" float4 (make-array Float/TYPE 4))

(defn image-raster ^Raster [^BufferedImage img]
  (.. img getRaster))

(defn raster-data [img]
  (.getData ^DataBufferByte (.getDataBuffer (image-raster img))))

(defn read-img [path]
  (ImageIO/read (io/file path)))

(defn bytes->img [bytes]
  (ImageIO/read (ByteArrayInputStream. bytes)))

(defn write-png [path img]
  (ImageIO/write
   ^java.awt.image.RenderedImage img
   "png"
   (io/file path)))

(defn slice [img [x1 y1 x2 y2]]
  (let [src-raster (image-raster img)
        width (int (Math/ceil (inc (- x2 x1))))
        height (int (Math/ceil (inc (- y2 y1))))
        dest (BufferedImage. width height BufferedImage/TYPE_INT_ARGB)
        ^java.awt.image.WritableRaster dest-raster (image-raster dest)]
    (doseq [x (range width)
            y (range height)
            px (.getPixel src-raster (int (+ x1 x)) (int (+ y1 y)) float4)]
      (try
        (.setPixel dest-raster (int x) (int y) float4)
        (catch ArrayIndexOutOfBoundsException e
          (println "out of bounds:" [x1 y1 x2 y2] [x y] :width width :height height)
          (throw e))))
    dest))

(defn width [^BufferedImage img]
  (.getWidth img))

(defn height [^BufferedImage img]
  (.getHeight img))

(defn new-img ^BufferedImage [w h]
  (BufferedImage. w h BufferedImage/TYPE_INT_ARGB))

(defn apply-transform [^BufferedImage in ^BufferedImage out ^AffineTransform at]
  (let [^AffineTransformOp at-op (AffineTransformOp. at AffineTransformOp/TYPE_BILINEAR)]
    (.filter at-op in out)))

(defn scale-img [^BufferedImage img scale]
  (apply-transform
   img
   (new-img (* (width img) scale)
            (* (height img) scale))
   (AffineTransform/getScaleInstance scale scale)))

(defn rotate-180 [^BufferedImage img]
  (apply-transform
   img
   (new-img (width img) (height img))
   (AffineTransform/getRotateInstance Math/PI (/ (width img) 2) (/ (height img) 2))))

(defn rotate-clockwise [^BufferedImage img]
  (let [h (height img) w (width img)]
    (apply-transform
     img
     (new-img h w)
     (doto (AffineTransform.)
       (.translate (/ h 2) (/ w 2))
       (.rotate (/ Math/PI 2))
       (.translate (- (/ w 2)) (- (/ h 2)))))))

(defn rotate-counterclockwise [^BufferedImage img]
  (let [h (height img) w (width img)]
    (apply-transform
     img
     (new-img h w)
     (doto (AffineTransform.)
       (.translate (/ h 2) (/ w 2))
       (.rotate (* 3/2 Math/PI))
       (.translate (- (/ w 2)) (- (/ h 2)))))))

(defn image-png-bytes [^BufferedImage img]
  (let [baos (ByteArrayOutputStream.)]
    (ImageIO/write img "png" baos)
    (.toByteArray baos)))
