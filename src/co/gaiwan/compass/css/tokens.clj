(ns co.gaiwan.compass.css.tokens
  {:ornament/prefix ""}
  (:require
   [charred.api :as charred]
   [clojure.java.io :as io]
   [garden.stylesheet :as gs]
   [lambdaisland.ornament :as o]))

(o/import-tokens! (charred/read-json (io/resource "open-props.tokens.json")) {:include-values? false})

(o/defprop --hoc-pink "#e25f7d")
(o/defprop --surface-1)
(o/defprop --surface-2)
(o/defprop --surface-3)
(o/defprop --surface-4)

(o/defprop --talk-color)
(o/defprop --workshop-color)
(o/defprop --office-hours-color)
(o/defprop --activity-color)

(o/defrules session-colors
  [":where(html)"
   {--talk-color         --blue-2
    --workshop-color     --teal-2
    --office-hours-color --red-2
    --activity-color     --red-2}]

  (gs/at-media
   {:prefers-color-scheme 'dark}
   [":where(html)"
    {--talk-color         --blue-9
     --workshop-color     --teal-8
     --office-hours-color --red-9
     --activity-color     --red-9}]))
