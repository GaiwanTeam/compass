(ns co.gaiwan.compass.css
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [co.gaiwan.compass.css.colors :as colors]
   [lambdaisland.ornament :as o]))

(def simple-tokens
  {:surface-1 {:background-color "var(--surface-1)"}
   :surface-2 {:background-color "var(--surface-2)"}
   :surface-3 {:background-color "var(--surface-3)"}
   :surface-4 {:background-color "var(--surface-4)"}
   :flex      {:display "flex"}
   :flex-col  {:display        "flex"
               :flex-direction "column"}})

(def components
  [{:id :shadow
    :rules
    "shadow = <'shadow'> (<'-'> shadow-size)?
     shadow-size = #'[0-9]+'"
    :garden (fn [{:keys [component-data]}]
              (let [shadow-size (or (second (first component-data)) 1)]
                {:box-shadow (str "var(--shadow-" shadow-size ")")}))}

   {:id :margin
    :rules
    "margin = <'margin'> (<'-'> margin-type)? <'-'> margin-size
      margin-type = #'[trbl]+'
      margin-size = #'[0-9]+'"
    :garden (fn [{:keys [component-data]}]
              (let [{:keys [margin-type margin-size]} (into {} component-data)
                    sides (set margin-type)
                    size (str "var(--size-" margin-size ")")]
                (if (or (not margin-type) (= #{\t \b \l \r} sides))
                  {:margin size}
                  (cond-> {}
                    (sides \t)
                    (assoc :margin-top size)
                    (sides \r)
                    (assoc :margin-right size)
                    (sides \b)
                    (assoc :margin-bottom size)
                    (sides \l)
                    (assoc :margin-left size)))))}
   {:id :padding
    :rules
    "padding = <'padding'> (<'-'> padding-type)? <'-'> padding-size
      padding-type = #'[trbl]+'
      padding-size = #'[0-9]+'"
    :garden (fn [{:keys [component-data]}]
              (let [{:keys [padding-type padding-size]} (into {} component-data)
                    sides (set padding-type)
                    size (str "var(--size-" padding-size ")")]
                (if (or (not padding-type) (= #{\t \b \l \r} sides))
                  {:padding size}
                  (cond-> {}
                    (sides \t)
                    (assoc :padding-top size)
                    (sides \r)
                    (assoc :padding-right size)
                    (sides \b)
                    (assoc :padding-bottom size)
                    (sides \l)
                    (assoc :padding-left size)))))}])

(o/set-tokens!
 {:colors colors/girouette-tokens
  :components
  (with-meta
    (into components
          (for [[t g] simple-tokens]
            {:id t
             :garden g}))
    {:replace true})})

(defn spit-styles []
  (spit "resources/public/css/styles.css"
        (o/defined-styles)))

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
