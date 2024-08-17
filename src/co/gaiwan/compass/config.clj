(ns co.gaiwan.compass.config
  "Configuration API

  Gives us just enough flexiblity to comfortably have separate prod/edn
  config+secrets, maximally checking in anything that isn't a secret into the
  repo, and allowing for convenient local overrides.

  Main public API: `config/value`, the rest is plumbing.

  Merge in order (where applicable)

  - `resources/co/gaiwan/compass/config.edn` - Base config file, shared values
  - `resources/co/gaiwan/compass/prod.edn`   - Prod specific values
  - `resources/co/gaiwan/compass/dev.edn`    - Dev specific values
  - `config.local.edn`                       - Local overrides, does not get checked in
  - environment variables                    - After munging, e.g. :f-oo/bar -> F_OO__BAR

  When running with launchpad config files are watched for changes.
  "
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def config {})
(def env :dev)

(def config-edn (io/resource "co/gaiwan/compass/config.edn"))
(def extra-config-files ["config.local.edn"])

(defn env-edn [env]
  (io/resource (str "co/gaiwan/compass/" (name env) ".edn")))

(defn load-config
  []
  (apply
   merge
   (aero/read-config config-edn {})
   (aero/read-config (env-edn env))
   (for [f extra-config-files]
     (when (.exists (io/file f))
       (aero/read-config f {})))))

(defn load-config! [& _]
  (alter-var-root #'config (constantly (load-config))))

(defn set-env! [e]
  (alter-var-root #'env (constantly e))
  (load-config!))

(defn add-config-file! [f]
  (alter-var-root #'extra-config-files conj f)
  (load-config!))

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
     {(str config-edn)   #'load-config!
      (env-edn env)      #'load-config!
      "config.local.edn" #'load-config!})))
