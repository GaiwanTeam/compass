(ns co.gaiwan.compass.routes.ticket
  (:require
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.http.response :as response]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.services.tito :as tito]
   [co.gaiwan.compass.util :as util]))

(defn GET-connect-ticket-form
  [{:keys [identity] :as req}]
  (if-let [ticket (first  (:tito.ticket/_assigned-to identity))]
    {:html/body
     [:div
      [:h2 "Ticket Connection"]
      [:p "Your ti.to ticket is already connected! You're all set!"]
      [:p "Your ticket reference is " [:strong (:tito.ticket/reference ticket)] "."]]}
    {:html/body
     [:div
      [:h2 "Ticket Connection"]
      [:p "Connect your tito ticket to your compass account here by entering the reference code and your email address!"]
      [:form {:method "post"}
       [:label {:for "reference"} "The ticket reference code:"] [:br]
       [:input {:type "text" :required true :name "reference" :maxlength 4 :placeholder "QUTU"}] [:br]
       [:label {:for "email"} "The email address assigned to the ticket:"] [:br]
       [:input {:type "email" :required true :name "email" :value (:discord/email identity)}] [:br]
       [:input {:type "submit" :value "Connect"}]]]}))

(defn POST-connect-ticket-form
  [{:keys [identity]
    {ref "reference" email "email"} :form-params
    :as req}]
  (if (and ref email)
    (if-let [ticket (tito/find-unassigned-ticket (str/upper-case ref) email)]
      (do
        @(db/transact
          [[:db/add (:db/id ticket) :tito.ticket/assigned-to [:user/uuid (:user/uuid identity)]]])
        (discord/assign-ticket-role (:discord/id identity) ticket)
        (response/redirect "/" {:flash [:p "Ticket connection successful! You should now have the appropriate roles in our Discord server."]}))
      (response/redirect "/connect-ticket" {:status :found
                                            :flash [:p {:style "color: red;"} "That did not work. Are you sure the ticket reference "
                                                    [:code (str/upper-case ref)] " is correct and assigned to " [:code email] "?"]}))
    {:status 400
     :html/body "Missing parameters"}))

(defn routes []
  [["/connect-ticket"
    {:middleware [[response/wrap-requires-auth]]}
    [""
     {:get {:handler #'GET-connect-ticket-form}
      :post {:handler #'POST-connect-ticket-form}}]]])
