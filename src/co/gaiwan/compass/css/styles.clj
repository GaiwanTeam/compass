(ns co.gaiwan.compass.css.styles
  (:require [lambdaisland.ornament :as o]))

(o/defrules resets
  [[#{:ul :ol} :list-none :m-0 :p-0]
   [:body :overflow-x-hidden :w-screen]])
