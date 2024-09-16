(ns lambdaisland.wagontrain
  (:require
   [datomic.api :as d]
   [io.pedestal.log :as log]))

(def schema
  [{:db/ident       :wagontrain.tx/label,
    :db/valueType   :db.type/keyword,
    :db/doc         "Label identifiying the transaction, so it only gets applied once."
    :db/cardinality :db.cardinality/one}
   {:db/ident       :wagontrain.tx/direction,
    :db/valueType   :db.type/keyword,
    :db/doc         "Is this an up or a down migration?",
    :db/cardinality :db.cardinality/one}
   {:db/ident       :wagontrain.tx/reverses,
    :db/valueType   :db.type/ref,
    :db/doc         "Reference to a past transaction that this migration reverses",
    :db/cardinality :db.cardinality/one}
   {:db/ident       :wagontrain.tx/reversed-by,
    :db/valueType   :db.type/ref,
    :db/doc         "Reference to a future transaction that reverses this transaction.",
    :db/cardinality :db.cardinality/one}])

(defn up-tx [label tx-data]
  (into
   [{:db/id "datomic.tx"
     :wagontrain.tx/direction :up
     :wagontrain.tx/label label}]
   tx-data))

(defn down-tx [label up-tx-id tx-data]
  (into
   [{:db/id "datomic.tx"
     :wagontrain.tx/direction :down
     :wagontrain.tx/label label
     :wagontrain.tx/reverses up-tx-id}
    {:db/id up-tx-id
     :wagontrain.tx/reversed-by "datomic.tx"}]
   tx-data))

(defn up-tx-id [conn label]
  (d/q '[:find ?tx .
         :in $ ?label
         :where
         [?tx :wagontrain.tx/label ?label]
         [?tx :wagontrain.tx/direction :up]
         (not [?tx :wagontrain.tx/reversed-by])]
       (d/db conn)
       label))

(defn applied? [conn label]
  (boolean (up-tx-id conn label)))

(defn migrate1
  [conn {:keys [label tx-data]}]
  (if (applied? conn label)
    (log/info :migration/skipped {:label label})
    (let [{:keys [tx-data db-after]} @(d/transact conn (up-tx label (if (fn? tx-data) (tx-data) tx-data)))]
      (log/info :migration/applied {:label label
                                    :datom-count (count tx-data)
                                    :basis-t (d/basis-t db-after)}))))

(defn migrate! [conn migrations]
  (run! (partial migrate1 conn) migrations))

(defn rollback!
  [conn label]
  (let [tx-id (up-tx-id conn label)]
    (if-not tx-id
      (throw (ex-info {:label label} "Migration not found"))
      (let [datoms (seq (:data (first (d/tx-range (d/log conn) tx-id nil))))]
        @(d/transact
          conn
          (down-tx
           label
           tx-id
           (for [[e a v t add?] (remove #(= tx-id (.e ^datomic.Datom %)) datoms)]
             [(if add? :db/retract :db/add) e a v])))))))
