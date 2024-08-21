(ns co.gaiwan.compass.routes.ticket
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.services.tito :as tito]
   [co.gaiwan.compass.routes.sessions :refer [GET-sessions]]
   [co.gaiwan.compass.util :as util]
   [lambdaisland.hiccup :as hiccup]
   [clojure.pprint :as pprint]))

(defn connect-ticket-form
  ([error-message ref email]
   [:section
    (when error-message
      [:p {:style "color: red;"} error-message])
    [:h2 "Ticket Connection"]
    [:p "Connect your tito ticket to your compass account here by entering the reference code and your email address!"]
    [:form {:method "post"}
     [:label {:for "reference"} "The ticket reference code:"] [:br]
     [:input (cond-> {:type "text" :required true :name "reference" :maxlength 4 :placeholder "4-character registration reference"}
               ref (assoc :value ref))]
     [:br]
     [:label {:for "email"} "The email address assigned to the ticket:"] [:br]
     [:input {:type "email" :required true :name "email" :value email}] [:br]
     [:input {:type "submit" :value "Connect"}]]])
  ([ref email]
   (connect-ticket-form nil ref email)))

(defn GET-connect-ticket-form
  [{:keys [identity] :as req}]
  (if-let [ticket (first  (:tito.ticket/_assigned-to identity))]
    {:html/body
     [:div
      [:h2 "Ticket Connection"]
      [:p "Your ti.to ticket is already connected! You're all set!"]
      [:p "Your ticket reference is " [:strong (:tito.ticket/reference ticket)] "."]]}
    {:html/body
     (connect-ticket-form nil (:email (discord/fetch-user-info (oauth/current-access-token identity))))}))

(defn POST-connect-ticket-form
  [{:keys [identity]
    {ref "reference" email "email"} :form-params
    :as req}]
  (if (and ref email)
    (let [ref (str/upper-case ref)
          ticket-state (fn [& states] (comp (set states) :tito.ticket/state))
          tickets (tito/find-tickets ref)
          unassigned-refs (->> tickets
                               (filter (ticket-state "new" "reminder"))
                               (map :tito.ticket/reference))
          assigned-ticket (->> tickets
                               (filter (ticket-state "complete" "incomplete"))
                               (filter (comp #{email} :tito.ticket/email))
                               first)
          error-response (fn [status & msg]
                           {;; TODO: styling, use alternative flash style (error)
                            :html/body (connect-ticket-form msg ref email)})]
      (cond
        (:tito.ticket/_assigned-to identity)
        (error-response 409 "A ticket is already assigned to your account!")

        assigned-ticket
        (do
          (discord/assign-ticket-role (:discord/id identity) assigned-ticket)
          @(db/transact
            [[:db/add (:db/id assigned-ticket) :tito.ticket/assigned-to [:user/uuid (:user/uuid identity)]]])
          (response/redirect
           "/"
           {:flash [:p "Ticket connection successful! You should now have the appropriate roles in our Discord server."]}))

        (empty? tickets)
        (error-response 404 "Registration reference " [:code ref] " not found.")

        (seq unassigned-refs)
        (error-response 404
                        "Registration " [:code ref] " found, but no ticket in it is assigned to your email address."
                        [:br] "The following tickets are not assigned to an email address yet (maybe you forgot to assign one of them to " [:code email] "?): "
                        (str/join "," unassigned-refs))

        :else
        (error-response 404 "Registration " [:code ref] " found, but no ticket in it is assigned to your email address.")))

    {:status 400
     :html/body "Missing parameters"}))

(defn routes []
  [["/connect-ticket"
    {:middleware [[response/wrap-requires-auth]]}
    [""
     {:get {:handler #'GET-connect-ticket-form}
      :post {:handler #'POST-connect-ticket-form}}]]])
