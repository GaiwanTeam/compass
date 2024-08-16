(ns co.gaiwan.compass.routes.ticket
  (:require
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.services.tito :as tito]
   [co.gaiwan.compass.util :as util]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [co.gaiwan.compass.db :as db]))

(defn GET-connect-ticket-form
  [{:keys [identity] :as req}]
  (if-not (empty? identity)
    {:html/body
     [:div
      [:h2 "Ticket Connection"]
      [:p "Connect your tito ticket to your compass account here by entering the reference code and your email address!"]
      [:form {:method "post"}
       [:label {:for "reference"} "The ticket reference code:"] [:br]
       [:input {:type "text" :required true :name "reference" :maxlength 4 :placeholder "QUTU"}] [:br]
       [:label {:for "email"} "The email address assigned to the ticket:"] [:br]
       [:input {:type "email" :required true :name "email" :value (:discord/email identity)}] [:br]
       [:input {:type "submit" :value "Connect"}]]]}
    (util/redirect (oauth/flow-init-url {:redirect-url "/connect-ticket"}) {:status :found})))

(defn POST-connect-ticket-form
  [{:keys [identity]
    {ref "reference" email "email"} :form-params
    :as req}]
  (if-not (empty? identity)
    (if (and ref email)
      (if-let [ticket (tito/find-unassigned-ticket (str/upper-case ref) email)]
        (do
          (db/transact
           [:db/add (:db/id ticket) :tito.ticket/assigned-to [:user/uuid (:user/uuid identity)]])
          (discord/assign-ticket-role (:discord/id identity) ticket)
          (util/redirect "/" {:flash [:p "Ticket connection successful! You should now have the appropriate roles in our Discord server."]}))
        (util/redirect "/connect-ticket" {:status :found
                                          :flash [:p {:style "color: red;"} "That did not work. Are you sure the ticket reference "
                                                  [:code (str/upper-case ref)] " is correct and assigned to " [:code email] "?"]}))
      {:status 400
       :html/body "Missing parameters"})
    (util/redirect (oauth/flow-init-url {:redirect-url "/connect-ticket"}))))

(defn routes []
  [["/connect-ticket"
    [""
     {:get {:handler #'GET-connect-ticket-form}
      :post {:handler #'POST-connect-ticket-form}}]]])
