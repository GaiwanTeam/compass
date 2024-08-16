(ns co.gaiwan.compass.routes.ticket
  (:require
   [co.gaiwan.compass.http.oauth :as oauth]
   [co.gaiwan.compass.services.discord :as discord]
   [co.gaiwan.compass.services.tito :as tito]
   [co.gaiwan.compass.util :as util]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(defn GET-connect-ticket-form
  [{{:keys [user/email] :as user} :identity :as req}]
  (if user
    {:html/body
     [:div
      [:h2 "Ticket Connection"]
      [:p "Connect your tito ticket to your compass account here by entering the reference code and your email address!"]
      [:form {:method "post"}
       [:label {:for "reference"} "The ticket reference code:"] [:br]
       [:input {:type "text" :required true :name "reference" :maxlength 4 :placeholder "QUTU"}] [:br]
       [:label {:for "email"} "The email address assigned to the ticket:"] [:br]
       [:input {:type "email" :required true :name "email" :value email}] [:br]
       [:input {:type "submit" :value "Connect"}]]]}
    (util/redirect (oauth/flow-init-url {:redirect-url "/connect-ticket"}))))

(defn POST-connect-ticket-form
  [{{discord-user-id :discord/id :as user} :identity
    {ref "reference" email "email"} :form-params
    :as req}]
  (if user
    (if (and ref email)
      (if-let [ticket (tito/find-assigned-ticket (str/upper-case ref) email)]
        (do
          (discord/assign-ticket-role discord-user-id ticket)
          (util/redirect "/" {:flash [:p "Ticket connection successful! You should now have the appropriate roles in our Discord server."]}))
        (util/redirect "/connect-ticket" {:flash [:p {:style "color: red;"} "That did not work. Are you sure the ticket reference "
                                                  [:code (str/upper-case ref)] " is correct and assigned to " [:code email] "?"]}))
      {:status 400
       :html/body "Missing parameters"})
    (util/redirect (oauth/flow-init-url {:redirect-url "/connect-ticket"}))))

(defn routes []
  [["/connect-ticket"
    [""
     {:get {:handler #'GET-connect-ticket-form}
      :post {:handler #'POST-connect-ticket-form}}]]])
