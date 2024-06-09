(ns co.gaiwan.compass.css.tokens
  {:ornament/prefix ""}
  (:require
   [lambdaisland.ornament :as o]
   [clojure.java.io :as io]
   [charred.api :as charred]))

(o/import-tokens! (charred/read-json (io/resource "open-props.tokens.json")))

(o/defprop --surface-1)
(o/defprop --surface-2)
(o/defprop --surface-3)
(o/defprop --surface-4)
(o/defutil surface-1 {:background-color --surface-1})
(o/defutil surface-2 {:background-color --surface-2})
(o/defutil surface-3 {:background-color --surface-3})
(o/defutil surface-4 {:background-color --surface-4})
(o/defutil flex      {:display "flex"})
(o/defutil flex-col  {:display        "flex"
                      :flex-direction "column"})
(o/defutil square    {:aspect-ratio 1})
(o/defutil rounded   {:border-radius "100%"})
(o/defutil w-full    {:width "100%"})
(o/defutil h-full    {:height "100%"})

(defn size-str [size unit]
  (if unit
    (str size unit)
    (str "var(--size-" size ")")) )

(def components
  [{:id     :shadow
    :rules
    "shadow = <'shadow'> (<'-'> shadow-size)?
     shadow-size = #'[0-9]+'"
    :garden (fn [{:keys [component-data]}]
              (let [shadow-size (or (second (first component-data)) 1)]
                {:box-shadow (str "var(--shadow-" shadow-size ")")}))}

   {:id     :margin
    :rules
    "margin = <'m'> (<'-'> margin-type)? <'-'> margin-size (margin-unit)?
     margin-type = #'[trbl]+'
     margin-size = #'[0-9]+'
     margin-unit = #'[%a-z]+'"
    :garden (fn [{:keys [component-data]}]
              (let [{:keys [margin-type margin-size margin-unit]} (into {} component-data)
                    sides                                         (set margin-type)
                    size                                          (size-str margin-size margin-unit)]
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
   {:id     :padding
    :rules
    "padding = <'p'> (<'-'> padding-type)? <'-'> padding-size (padding-unit)?
     padding-type = #'[trbl]+'
     padding-size = #'[0-9]+'
     padding-unit = #'[%a-z]+'"
    :garden (fn [{:keys [component-data]}]
              (let [{:keys [padding-type padding-size padding-unit]} (into {} component-data)
                    sides                                            (set padding-type)
                    size                                             (size-str padding-size padding-unit)]
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
                    (assoc :padding-left size)))))}

   {:id     :width
    :rules
    "width = <'w-'> width-size (width-unit)?
     width-size = #'[0-9]+'
     width-unit = #'[%a-z]+'"
    :garden (fn [{:keys [component-data]}]
              (let [{:keys [width-size width-unit]} (into {} component-data)]
                {:width (size-str width-size width-unit)}))}

   {:id     :height
    :rules
    "height = <'h-'> height-size (height-unit)?
     height-size = #'[0-9]+'
     height-unit = #'[%a-z]+'"
    :garden (fn [{:keys [component-data]}]
              (let [{:keys [height-size height-unit]} (into {} component-data)]
                {:height (if height-unit
                           (str height-size height-unit)
                           (str "var(--size-" height-size ")"))}))}])
