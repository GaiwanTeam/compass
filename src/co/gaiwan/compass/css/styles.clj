(ns co.gaiwan.compass.css.styles
  "Top level CSS rules"
  (:require
   [co.gaiwan.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defrules resets
  [
   [:p {:max-inline-size "inherit"}]
   [#{:ul :ol} :list-none :m-0 :p-0]
   [:body :overflow-x-hidden :w-screen]
   [#{:h1 :h2 :h3 :h4 :h5} {:max-inline-size "inherit"}]

   ;; override open-props normalize, we like the buttons a bit more rounded
   [#{:button :.btn} {:border-radius t/--radius-2}]

   ;; reset dialog
   [:dialog :p-0]

   [:body {:font-family "Open Sans, sans-serif"}]
   ])
