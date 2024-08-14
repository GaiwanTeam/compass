(ns co.gaiwan.compass.css.styles
  (:require [lambdaisland.ornament :as o]))

(o/defrules resets
  [[#{:ul :ol} :list-none :m-0 :p-0]
   [:body :overflow-x-hidden :w-screen]
   [#{:h1 :h2 :h3 :h4 :h5} {:max-inline-size "inherit"}]

   ;; override open-props normalize, we like the buttons a bit more rounded
   [#{:button :.btn} {:border-radius "0.4rem"}]

   ;; reset dialog
   [:dialog :p-0]

   [:body {:font-family "Open Sans, sans-serif"}]
   ])
