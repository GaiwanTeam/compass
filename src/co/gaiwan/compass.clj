(ns co.gaiwan.compass
  "The entrance point of the compass

  - read the config
  - manage the state and reloadable workflow
  "
  (:gen-class)
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [co.gaiwan.compass.config :as config]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl]
   [io.pedestal.log :as log]
   [lambdaisland.cli :as cli]))

(require
 'co.gaiwan.compass.db
 'co.gaiwan.compass.http
 'co.gaiwan.compass.css
 )

(set! *print-namespace-maps* false)

(defmethod aero/reader 'ig/ref [_ _tag value] (ig/ref value))
(defmethod aero/reader 'ig/refset [_ _tag value] (ig/refset value))
(defmethod aero/reader 'config [_ _tag k] (config/value k))

(def system-edn (io/resource "co/gaiwan/compass/system.edn"))
(def system-local-edn (io/file "system.local.edn"))

(defn ig-config
  "Get the integrant config map"
  ([]
   (ig-config :default))
  ([profile]
   (merge
    (aero/read-config system-edn {:profile profile})
    (when (.exists system-local-edn)
      (aero/read-config system-local-edn {:profile profile})))))

(defn set-prep!
  "Register out prep function with integrant-repl

  The prep-function will load the config, using the given profile, and let
  Integrant load any referenced namespaces."
  ([]
   (set-prep! :default))
  ([profile]
   (ig-repl/set-prep! #(doto (ig-config profile) ig/load-namespaces))
   (co.gaiwan.compass.css/spit-styles)))

(defn- add-shutdown-hook [f]
  (.addShutdownHook (java.lang.Runtime/getRuntime) (Thread. f)))

(defn go
  "Start the integrant system

  By default starts everything, pass a key or collection of keys to only start
  those keys. Registers a shutdown hook so stop handlers run when the
  application halts."
  ([]
   (go nil))
  ([{:keys [profile key]
     :or {profile :default}}]
   (log/info :compass/starting {:profile profile :key key})
   (set-prep! profile)
   (add-shutdown-hook
    (fn []
      (ig-repl/halt)
      (log/info :shutdown/finished {})))
   (cond
     (coll? key)
     (ig-repl/go key)
     (keyword? key)
     (ig-repl/go [key])
     (nil? key)
     (ig-repl/go))))

(def flags
  ["--env <prod|dev|test>" {:doc     "Configuration profile"
                            :default :prod
                            :parse   keyword}
   "--config <path>" {:doc   "Additional EDN file with configuration"
                      :coll? true}
   "--start <ig-key>" {:doc   "Which integrant component(s) to start"
                       :parse read-string}])

(defn set-config! [{:keys [config env]}]
  (when config
    (run! config/add-config-file! config))
  (config/set-env! env))

(defn run
  "Launch the Compass application"
  [opts]
  (set-config! opts)
  (go (cond-> {:profile (:env opts)}
        (:start opts)
        (assoc :key (:start opts)))))

(defn print-config
  "Print fully merged config and exit"
  [opts]
  (set-config! opts)
  (pprint/pprint
   (into {}
         (map (juxt identity config/value))
         (keys config/config))))

(def commands
  ["run" #'run
   "print-config" #'print-config])

(defn -main [& command-line-args]
  (cli/dispatch*
   {:name       "clojure -M -m co.gaiwan.compass"
    :flags      flags
    :commands   commands}
   command-line-args))
