(ns co.gaiwan.compass.model.session
  "(Mostly) pure functions over session values

  Computed properties and helpers functions."
  (:require
   [java-time.api :as time]))

(defn participating?
  "If user participates this session"
  [session user]
  (some (comp #{(:db/id user)} :db/id)
        (:session/participants session)))

(defn organizing?
  "If user organizes this session"
  [session user]
  (let [organized (:session/organized session)]
    (and
     ;; first make sure that user is already login
     (some? user)
     (or
     ;; Condition 1: organized property record the user's :db/id 
      (= (:db/id user)
         (:db/id organized))
     ;; Condition 2: organized property record the user's group :db/id
      (some (comp #{(:db/id user)} :db/id)
            (:user-group/users organized))
     ;; Condition 3: The user belongs to orga group 
      (some :user-group/orga
            (:user-group/_users user))))))

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
  (filter (fn [{:session/keys [participants organized]}]
            (or
             (= (:db/id user) (:db/id organized))
             (some (comp #{(:db/id user)} :db/id)
                   participants)))
          sessions))

(defmethod apply-filter :include-past [sessions user k v]
  (let [now (time/zoned-date-time)]
    (if v
      sessions
      (remove
       #(time/before? (:session/time %) now)
       sessions))))

(defmethod apply-filter :spots-available [sessions user k v]
  (filter (fn [{:session/keys [capacity participants]}]
            (< (count participants) capacity))
          sessions))



(defn apply-filters [sessions user filters]
  (def sessions sessions)
  (def f filters)
  (reduce
   (fn [sessions [k v]]
     (apply-filter sessions user k v))
   sessions
   filters))
