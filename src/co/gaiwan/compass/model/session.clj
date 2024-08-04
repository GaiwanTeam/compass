(ns co.gaiwan.compass.model.session
  "(Mostly) pure functions over session values

  Computed properties and helpers functions."
  (:require
   [java-time.api :as time]))

(defn participating? [session user]
  (some (comp #{(:db/id user)} :db/id)
        (:session/participants session)))

(defn organizing? [organized user]
  (and
   (some? organized)
   (= (:db/id user) (:db/id organized))))

f
;; => {:day :today,
;;     :type :all-types,
;;     :my-activities :my-activities,
;;     :include-past :include-past}

(defmulti apply-filter (fn [_ _ k _] k))

(defmethod apply-filter :default [sessions _ _ _]
  sessions)

(defmethod apply-filter :day [sessions _ k v]
  (case v
    :any-day
    sessions

    :today
    (filter
     (fn [{t :session/time}]
       (= (time/local-date)
          (time/local-date t)))
     sessions)

    :tomorrow
    (filter
     (fn [{t :session/time}]
       (= (time/+ (time/local-date) (time/days 1))
          (time/local-date t)))
     sessions)))

(defmethod apply-filter :type [sessions _ k v]
  (case v
    :all-types
    sessions

    :talks
    (filter (comp #{:session.type/talk :session.type/keynote}
                  :db/ident
                  :session/type)
            sessions)

    :activities
    (remove (comp #{:session.type/talk :session.type/keynote}
                  :db/ident
                  :session/type)
            sessions)))

(defmethod apply-filter :my-activities [sessions user k v]
  ;; TODO filter on
  ;; - user participates
  ;; - user created
  sessions)

(defmethod apply-filter :include-past [sessions user k v]
  (let [now (time/zoned-date-time)]
    (if v
      sessions
      (remove
       #(time/before? (:session/time %) now)
       sessions))))

(defn apply-filters [sessions user filters]
  (def sessions sessions)
  (def f filters)
  (reduce
   (fn [sessions [k v]]
     (apply-filter sessions user k v))
   sessions
   filters)
  )
