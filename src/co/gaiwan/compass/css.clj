(ns co.gaiwan.compass.css
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [lambdaisland.ornament :as o]))

(defn spit-styles [e]
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
  (spit "resources/public/css/styles.css"
        (o/defined-styles)))

(when-let [watch! (try (requiring-resolve 'lambdaisland.launchpad.watcher/watch!) (catch Exception _))]
  (watch!
   {"src" #'spit-styles}))
