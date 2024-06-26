(ns co.gaiwan.compass.css.components
  (:require
   [girouette.tw.accessibility :as accessibility]
   [girouette.tw.animation :as animation]
   [girouette.tw.background :as background]
   [girouette.tw.border :as border]
   [girouette.tw.box-alignment :as box-alignment]
   [girouette.tw.common :as common]
   [girouette.tw.effect :as effect]
   [girouette.tw.filter :as filter]
   [girouette.tw.flexbox :as flexbox]
   [girouette.tw.grid :as grid]
   [girouette.tw.interactivity :as interactivity]
   [girouette.tw.layout :as layout]
   [girouette.tw.sizing :as sizing]
   [girouette.tw.spacing :as spacing]
   [girouette.tw.svg :as svg]
   [girouette.tw.table :as table]
   [girouette.tw.transform :as transform]
   [girouette.tw.typography :as typography]
   ))

(def overrides
  [;; Use open props sizing
   {:id :font-size
    :rules "
    font-size = <'font-size-'> #\"0-9\"+
    "
    :garden (fn [{[font-size] :component-data}]
              {:font-size "var(--font-size-" font-size ")"})}

   ;; Use open props shadows
   {:id      :box-shadow
    :rules   "
    box-shadow = <'shadow'> (<'-'> box-shadow-value)?
    box-shadow-inner = 'inner'
    box-shadow-value = '1' | '2' | '3' | '4' | '5' | 'sm' | 'md' | 'lg' | 'xl' | 'none'
    "
    :garden  (fn [{data :component-data}]
               (let [{:keys [box-shadow-value box-shadow-inner]} (into {} data)
                     shadow-value (case box-shadow-value
                                    "1" 1
                                    "2" 2
                                    "3" 3
                                    "4" 4
                                    "5" 4
                                    nil 2
                                    "none" nil)]
                 {:box-shadow (if shadow-value
                                (if box-shadow-inner
                                  (str "var(--inner-shadow-" shadow-value ")")
                                  (str "var(--shadow-" shadow-value ")"))
                                "none")}))}

   ;; Make display:flex implied
   {:id :flex-direction
    :rules "
    flex-direction = <'flex-'> ('row' | 'row-reverse' | 'col' | 'col-reverse')
    "
    :garden (fn [{[direction] :component-data}]
              {:display "flex"
               :flex-direction ({"row" "row"
                                 "row-reverse" "row-reverse"
                                 "col" "column"
                                 "col-reverse" "column-reverse"} direction)})}

   ;; FIXME
   ;; Use line-height-* instead of leading-* , use open props sizing
   {:id :line-height
    :rules "
    line-height = <'line-height-'> #\"0-9\"+
    "
    :garden (fn [{[value-data] :component-data}]
              {:line-height
               (str "var(--font-lineheight-" (first value-data) ")")})}

   ;; Drop number from this rule, so the previous one kicks in
   {:id :line-height-2
    :rules "
    line-height-2 = <'line-height-'> (fraction | percentage)
    "
    :garden (fn [{[value-data] :component-data}]
              {:line-height
               (girouette.tw.common/value-unit->css
                value-data
                {:fraction {:unit "%"
                            :value-fn girouette.tw.common/mul-100}})})}])

(def extras
  {:id :small-caps
   :garden {:font-variant "small-caps"}})

(def girouette-components
  [#_ common/components
   layout/components ;; block, inline, overflow-hidden, static, fixed, absolute, z-1
   flexbox/components ;; flex, flex-col, grow, shrink
   grid/components
   box-alignment/components
   spacing/components
   sizing/components
   typography/components
   background/components
   border/components
   #_ effect/components
   #_ filter/components
   #_ table/components
   #_ animation/components
   #_ transform/components
   #_ interactivity/components
   #_ svg/components
   #_ accessibility/components
   overrides
   extras])

(defonce use-open-props-sizing
  (alter-var-root
   #'girouette.tw.common/value-unit->css
   (fn [orig]
     (fn value-unit->css
       ([data]
        (orig data))
       ([data {:keys [number] :as options}]
        (let [[data-type arg1] data]
          (if (and (= {:unit "rem" :value-fn girouette.tw.common/div-4} number)
                   (#{:integer :number} data-type))
            (str "var(--size-" arg1 ")")
            (orig data options))))))))

(defonce non-hex-colors
  (alter-var-root
   #'girouette.tw.color/read-color
   (fn [orig]
     (fn read-color [color-map color]
       (def color-map color-map)
       (def color color)
       (let [[_ [type param1 param2 param3 param4]] color]
         (if (not= :predefined-color-opacity type)
           (orig color-map color)
           (let [color-code (color-map param1)]
             (if (re-find #"^[0-9a-fA-F]{6}$" color-code)
               (orig color-map color)
               color-code))))))))
