(ns co.gaiwan.compass.util
  (:require
   [clojure.pprint :as pprint]))

(defn pprint-str [o]
  (with-out-str (pprint/pprint o)))
