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
