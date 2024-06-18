(ns co.gaiwan.compass.css.tokens
  {:ornament/prefix ""}
  (:require
   [lambdaisland.ornament :as o]
   [clojure.java.io :as io]
   [charred.api :as charred]))

(o/import-tokens! (charred/read-json (io/resource "open-props.tokens.json")) {:include-values? false})

(o/defprop --hoc-pink "#e25f7d")
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

;; openprops
(o/defprop --shadow-1)
(o/defprop --shadow-2)
(o/defprop --shadow-3)
(o/defprop --shadow-4)
(o/defprop --shadow-5)
(o/defprop --inner-shadow-0)
(o/defprop --inner-shadow-1)
(o/defprop --inner-shadow-2)
(o/defprop --inner-shadow-3)
(o/defprop --inner-shadow-4)
(o/defprop --inner-shadow-5)
