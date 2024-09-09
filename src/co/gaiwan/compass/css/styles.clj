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
   [#{:h1 :h2 :h3 :h4 :h5}
    {:color t/--text-1
     :max-inline-size "inherit"}]

   ;; override open-props normalize, we like the buttons a bit more rounded
   [#{:button :.btn} {:border-radius t/--radius-2}]

   ;; reset dialog
   [:dialog :p-0]

   [:body {:font-family "Open Sans, sans-serif"}]

   [:.site-copy
    [:p {:line-height 2}]
    [:h1 {:margin-top t/--size-5
          :margin-bottom t/--size-4}]
    [:h2 {:margin-top t/--size-4
          :margin-bottom t/--size-3}]
    [:h3 {:margin-top t/--size-3
          :margin-bottom t/--size-2}]
    [:h4 {:margin-top t/--size-2
          :margin-bottom t/--size-1}]
    [:h5 {:margin-top t/--size-1}]

    [:ul :py-2 [:li :py-1 :list-disc :list-inside]]
    [:ol :py-2 [:li :py-1 :list-decimal :list-inside]]]
   ])
