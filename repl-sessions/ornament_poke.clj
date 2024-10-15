(ns repl-sessions.ornament-poke
  (:require
   [clj-kondo.hooks-api :as api]
   [lambdaisland.ornament :as o]
   [lambdaisland.hiccup :as h]))

(comment
  ;; This is for debugging clj-kondo hooks
  ;; https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md
  (load-file ".clj-kondo/hooks/ornament.clj"))

(comment
  (hooks.ornament/defstyled
    {:node (api/parse-string
            "(o/defstyled action-button :button
               {:color \"red\"}
               [:&:hover {:color \"black\"}])
            ")})

  (hooks.ornament/defstyled
    {:node (api/parse-string
            "(o/defstyled action-button :button
               {:color \"red\"}
               [:&:hover {:color \"black\"}]
            ([a b c]
   [:<>
    [:a {:class [action-button (when active? \"active\")]} a]
    [:p.subtitle b]
    [:p.content c]]))
            ")}))

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
  [my-compo]
  ([]
   [my-compo "a" "b"
    [:h1.title "xxxx"]]))

(my-compo "hello")

(h/render
 [my-compo "a" "b"
  [:h1.title "xxxx"]])

class= "foo bar baz"

(str/join " " (cond-> ["foo" "bar" "baz"] active? (conj "active")))
