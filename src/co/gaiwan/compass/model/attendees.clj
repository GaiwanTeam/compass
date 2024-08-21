(ns co.gaiwan.compass.model.attendees)

(defn user-list
  " filter the user which is
    - has a tito.ticket
    - does not set public-profile/hidden? as true
  "
  [all-users]
  (filter
   (fn [{:public-profile/keys [hidden?]
         :tito.ticket/keys [_assigned-to]}]
     (and
      _assigned-to
      (not hidden?)))
   all-users))
