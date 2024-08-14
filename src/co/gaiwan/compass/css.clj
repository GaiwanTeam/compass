(ns co.gaiwan.compass.css
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.css.colors :as colors]
   [co.gaiwan.compass.css.components :as components]
   [co.gaiwan.compass.css.tokens :as tokens]
   [garden.compiler :as gc]
   [lambdaisland.ornament :as o]))

(require 'co.gaiwan.compass.css.styles
         )

(o/set-tokens!
 {:components (with-meta components/girouette-components {:replace true})
  :colors {"surface-1" "var(--surface-1)"
           "surface-2" "var(--surface-2)"
           "surface-3" "var(--surface-3)"
           "surface-4" "var(--surface-4)"}} )

(defn spit-styles []
  (spit "resources/public/css/styles.css"
        (gc/compile-css
         {:pretty-print? true}
         (o/defined-garden))))

(defn on-watcher-event [e]
  (when (.isFile (io/file (str (:path e))))
    (when (.endsWith (str (:path e)) ".clj")
      (require
       (symbol
        (-> (str (:path e))
            (str/replace #".*/src/" "")
            (str/replace #"/" ".")
            (str/replace #"_" "-")
            (str/replace #"\.clj$" "")))
       :reload)))
  (spit-styles))

(defonce install-watcher
  (when-let [watch! (try (requiring-resolve 'lambdaisland.launchpad.watcher/watch!) (catch Exception _))]
    (watch!
     {"src" #'on-watcher-event})))
