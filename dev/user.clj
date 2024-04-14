(ns user)

(defmacro jit [sym]
  `(requiring-resolve '~sym))

(defn go [& args]
  (apply (jit co.gaiwan.compass/go) args))

(defn reset []
  ((jit integrant.repl/reset)))

(defn reset-all []
  ((jit integrant.repl/reset-all)))

(defn browse []
  ((jit clojure.java.browse/browse-url) "http://localhost:8099"))

(defn conn []
  (:compass/db @(jit integrant.repl.state/system)))
