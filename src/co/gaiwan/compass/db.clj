(ns co.gaiwan.compass.db
  (:require
   [charred.api :as charred]
   [honey.sql :as honey]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]
   [next.jdbc.prepare :as prepare]
   [next.jdbc.quoted :rename {postgres identifier}]
   [next.jdbc.result-set :as rs]
   [next.jdbc.sql :as sql])
  (:import
   (java.sql PreparedStatement)
   (org.postgresql.util PGobject)))

(def default-cols
  [[:id         :bigserial :primary-key]
   [:created-at :timestamptz [:default [:now]]]
   [:updated-at :timestamptz [:default [:now]]]
   [:props      :jsonb]])

(def tables
  {:user
   [[:email :text [:not nil] :unique]
    [:name :text]]
   })

(defn metaschema [db]
  (jdbc/on-connection [conn db]
    (let [metadata (.getMetaData conn)
          catalog  (.getCatalog conn)
          rs  (.getTables metadata nil nil nil (into-array ["TABLE" "VIEW"]))]
      (into
       {}
       (for [table (loop [tables []]
                     (if (.next rs)
                       (recur (conj tables (.getString rs "TABLE_NAME")))
                       tables))
             :let [cols (-> (.getMetaData conn)
                            (.getColumns nil nil table nil)
                            (rs/datafiable-result-set nil)
                            (doto println))]]
         [(keyword table)
          (for [{:keys [COLUMN_NAME TYPE_NAME]} cols]
            [(keyword COLUMN_NAME)
             (keyword TYPE_NAME)])])))))

(defn migrate [db schema]
  (let [ms (metaschema db)]
    (doseq [[table columns] schema]
      (if (contains? ms table)
        (let [old-cols (into #{} (map first) (get ms table))
              new-cols (remove #(contains? old-cols (first %)) columns)]
          (doseq [col new-cols]
            (jdbc/execute!
             (jdbc/get-datasource db)
             (doto (honey/format
                    {:alter-table table
                     :add-column col}
                    {:quoted true})
               prn))))
        (jdbc/execute!
         (jdbc/get-datasource db)
         (honey/format
          {:create-table table
           :with-columns columns}
          {:quoted true}))))))

;;;;;; Integrant

(defmethod ig/init-key :compass/db [_ {:keys [url]}]
  (migrate url (for [[table cols] tables]
                 [table (concat default-cols cols)]))
  {:url url
   :schema (metaschema url)})

(defmethod ig/halt-key! :compass/db [_ conn]
  )

;;;; JSONB stuff

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (charred/write-json-str x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (charred/read-json value :key-fn keyword)
          {:pgtype type}))
      value)))

(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))




(comment
  (def URL_COMPASS "jdbc:pgsql://localhost:5432/compass?user=postgres")
  (metaschema URL_COMPASS)

  (migrate URL_COMPASS (for [[table cols] tables]
                         [table (concat default-cols cols)]))

  (sql/insert! (jdbc/get-datasource URL_COMPASS)
               :user
               {:user/email "arne@gaiwan.co"
                :user/props {:foo 123}}
               {:column-fn pr-str
                :table-fn pr-str})

  (jdbc/execute-one!
   (jdbc/get-datasource URL_COMPASS)
   (next.jdbc.sql.builder/for-insert
    :user
    {:user/email "arne@gaiwan.co"
     :user/props {:foo 123}}
    {:column-fn pr-str
     :table-fn pr-str}))

  (sql/query URL_COMPASS ["select * from \"user\""])


  (def URL_ADMIN   "jdbc:pgsql://localhost:5432/postgres?user=postgres")
  (def URL_COMPASS "jdbc:pgsql://localhost:5432/compass?user=postgres")

  (defn recreate-db!
    ([db-name]
     (recreate-db! URL_ADMIN db-name))
    ([connect-url db-name]
     (let [ds (jdbc/get-datasource connect-url)]
       (jdbc/execute! ds [(str "DROP DATABASE IF EXISTS " (identifier db-name))])
       (jdbc/execute! ds [(str "CREATE DATABASE " (identifier db-name))]))))

  (recreate-db! "compass")

  (let [ds (jdbc/get-datasource URL_COMPASS)]
    (jdbc/execute! ds [(str "CREATE TABLE users (email TEXT NOT NULL)")]))



  )
