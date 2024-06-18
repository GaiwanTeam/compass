(ns co.gaiwan.compass.css
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.css.colors :as colors]
   [co.gaiwan.compass.css.tokens :as tokens]
   [co.gaiwan.compass.css.styles :as styles]
   [garden.compiler :as gc]
   [lambdaisland.ornament :as o]))

(defn spit-styles []
  (spit "resources/public/css/styles.css"
        (gc/compile-css
         {:pretty-print? false}
         (concat
          styles/styles
          (o/defined-garden)))))

(defn on-watcher-event [e]
  (when (.isFile (io/file (str (:path e))))
    (when (.endsWith (str (:path e)) ".clj")
      (require
       (doto
           (symbol
            (-> (str (:path e))
                (str/replace #".*/src/" "")
                (str/replace #"/" ".")
                (str/replace #"_" "-")
                (str/replace #"\.clj$" "")))
         prn)
       :reload)))
  (spit-styles))

(defonce install-watcher
  (when-let [watch! (try (requiring-resolve 'lambdaisland.launchpad.watcher/watch!) (catch Exception _))]
    (watch!
     {"src" #'on-watcher-event})))
