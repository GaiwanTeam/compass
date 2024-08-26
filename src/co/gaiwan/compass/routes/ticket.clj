(ns co.gaiwan.compass.routes.ticket
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.html.ticket :as ticket]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.services.tito :as tito]))

(defn GET-connect-ticket-form
  [{:keys [identity] :as req}]
  (if-let [[ticket] (:tito.ticket/_assigned-to identity)]
    {:html/body [ticket/ticket-connected ticket]}
    {:html/body [ticket/connect-ticket-form nil (:discord/email identity)]}))

(defn POST-connect-ticket-form
  [{:keys                           [identity]
    {ref "reference" email "email"} :form-params
    :as                             req}]
  (if (and ref email)
    (let [ref             (subs (str/upper-case ref) 0 4)
          ticket-state    (fn [& states] (comp (set states) :tito.ticket/state))
          tickets         (tito/find-tickets ref)
          unassigned-refs (->> tickets
                               (filter (ticket-state "new" "reminder"))
                               (map :tito.ticket/reference))
          assigned-ticket (->> tickets
                               (filter (ticket-state "complete" "incomplete"))
                               (filter (comp #{email} :tito.ticket/email))
                               first)
          error-response  (fn [status & msg]
                            {:html/body [ticket/connect-ticket-form msg ref email]})]
      (cond
        (:tito.ticket/_assigned-to identity)
        (error-response 409 "A ticket is already assigned to your account!")

        assigned-ticket
        (if (discord/assign-ticket-roles (:discord/id identity) assigned-ticket)
          (do
            @(db/transact
              [[:db/add (:db/id assigned-ticket) :tito.ticket/assigned-to [:user/uuid (:user/uuid identity)]]])
            (response/redirect
             "/"
             {:flash [:p "Ticket connection successful! You should now have the appropriate roles in our Discord server."]}))
          (error-response 500 "Your data is correct, but the ticket roles could not be assigned to you. This is a bug; please contact the administrators."))

        (empty? tickets)
        (error-response 404 "Registration reference " [:code ref] " not found.")

        (seq unassigned-refs)
        (error-response 404
                        "Registration " [:code ref] " found, but no ticket in it is assigned to your email address."
                        [:br] "The following tickets are not assigned to an email address yet (maybe you forgot to assign one of them to " [:code email] "?): "
                        (str/join "," unassigned-refs))

        :else
        (error-response 404 "Registration " [:code ref] " found, but no ticket in it is assigned to your email address.")))

    {:status    400
     :html/body "Missing parameters"}))

(defn routes []
  ["/ticket"
   {:middleware [[response/wrap-requires-auth]]}
   ["/connect"
    {:name :ticket/connect
     :get {:handler #'GET-connect-ticket-form}
     :post {:handler #'POST-connect-ticket-form}}]])
