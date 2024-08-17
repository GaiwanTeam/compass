(ns co.gaiwan.compass.html.components
  "Generic components"
  (:require
   [co.gaiwan.compass.css.tokens :as t]
   [lambdaisland.ornament :as o]))

(o/defprop --toggle-radius-left t/--radius-2)
(o/defprop --toggle-radius-right t/--radius-2)

(o/defstyled toggle-button :label
  "Toggle implemented as a checkbox (can also be used as a radio button)."
  {:color t/--text-2}
  [:input :hidden]
  [:>.btn :w-full
   {:border-top-left-radius     --toggle-radius-left
    :border-bottom-left-radius  --toggle-radius-left
    :border-top-right-radius    --toggle-radius-right
    :border-bottom-right-radius --toggle-radius-right}]
  ["input:checked ~ .btn"
   :font-semibold
   {:color            t/--text-1
    :background-color t/--highlight}]
  ([props & children]
   (let [id (:id props)]
     [:<>
      {:for id}
      [:input
       (merge
        {:type    "checkbox"
         :name    id
         :value   id
         :checked (when (:checked? props) "checked")}
        (dissoc props :checked?))]
      [:div.btn
       children]])))

(o/defstyled toggle-group :div
  :flex :flex-row
  "Multiple toggle buttons that act as a group (implemented as radio buttons)"
  {--toggle-radius-left 0
   --toggle-radius-right 0}
  [":first-child > .btn" {--toggle-radius-left "0.5em"}]
  [":last-child > .btn" {--toggle-radius-right "0.5em"}]
  ([props]
   (for [[k v] (:options props)]
     [toggle-button (assoc (dissoc props :options :value)
                           :id (name k)
                           :type "radio"
                           :checked? (= (:value props) k)) v])))
