(ns co.gaiwan.compass
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl]
   [co.gaiwan.compass.config :as config]
   [io.pedestal.log :as log]))

(require
 'co.gaiwan.compass.db
 'co.gaiwan.compass.http
 'co.gaiwan.compass.css
 )

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
   (ig-repl/set-prep! #(doto (ig-config profile) ig/load-namespaces))))

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
