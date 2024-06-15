(ns co.gaiwan.compass.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def config {})
(def env (if-let [env (System/getenv "COMPASS_ENV")]
           (keyword env)
           :dev))

(def config-edn (io/resource "co/gaiwan/compass/config.edn"))
(def config-local-edn (io/file "config.local.edn"))

(defn env-edn [env]
  (io/resource (str "co/gaiwan/compass/" (name env) ".edn")))

(defn load-config
  [env]
  (merge
   (aero/read-config config-edn {})
   (aero/read-config (env-edn env))
   (when (.exists config-local-edn)
     (aero/read-config config-local-edn {}))))

(defn load-config! [& _]
  (intern *ns* 'config (load-config env)))

(defn key->env-var
  "Take the key used to identify a setting or secret, and turn it into a string
  suitable for use as an environment variable.

  - if the key is already a string it is left untouched
  - otherwise it is assumed to be an ident (symbol or keyword)
  - identifiers are uppercased and munged, as per [[munge]]
  - dashes become underscores
  - if the ident is qualified (has a namespace), two underscores are used to
    separate name and namespace"
  [k]
  (if (string? k)
    k
    (str (when (qualified-ident? k)
           (str (str/upper-case (munge (namespace k)))
                "__"))
         (str/upper-case (munge (name k))))))

(defn munge-env-value [s]
  (if (re-find #"^[0-9]+$" s)
    (parse-long s)
    s))

(defn value [k]
  (or (some-> (System/getenv (key->env-var k))
              munge-env-value)
      (get config k)))

(load-config!)

(defonce install-watcher
  (when-let [watch! (try (requiring-resolve 'lambdaisland.launchpad.watcher/watch!) (catch Exception _))]
    (watch!
     {(str config-edn)       #'load-config!
      (env-edn env)          #'load-config!
      (str config-local-edn) #'load-config!})))


