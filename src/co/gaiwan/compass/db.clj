(ns co.gaiwan.compass.db
  (:require
   [datomic.api :as d]
   [integrant.core :as ig]
   )
  )

(d/create-database "datomic:mem://compass")

;;;;;; Integrant

(defmethod ig/init-key :compass/db [_ {:keys [url]}]
  (d/connect url))

(defmethod ig/halt-key! :compass/db [_ conn]
  )
