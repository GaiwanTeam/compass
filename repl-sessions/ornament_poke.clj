(ns repl-sessions.ornament-poke
  (:require
   [lambdaisland.ornament :as o]
   [lambdaisland.hiccup :as h]))

(o/defstyled action-button :button
  {:color "red"}
  [:&:hover {:color "black"}])

(o/defstyled main-button action-button
  {:margin "1rem"})

(o/defstyled my-compo :nav
  [:>.title {:color "blue"}]
  [:>.subtitle {:font-weight "500"}]
  ([a b c]
   [:<>
    [:a {:class [action-button (when active? "active")]} a]
    [:p.subtitle b]
    [:p.content c]]))

(def active? true)

(o/defstyled wrapper :section
  [my-compo ]
  ([]
   [my-compo "a" "b"
    [:h1.title "xxxx"]]))

(my-compo "hello")

(h/render
 [my-compo "a" "b"
  [:h1.title "xxxx"]])

class="foo bar baz"

(str/join " " (cond-> ["foo" "bar" "baz"] active? (conj "active")))
