(ns co.gaiwan.compass.html.ticket)

(defn connect-ticket-form
  ([error-message ref email]
   [:section
    (when error-message
      [:p {:style "color: red;"} error-message])
    [:h2 "Ticket Connection"]
    [:p "Claim your conference ticket! This will unlock full access to Compass and Discord."]
    [:form {:method "post"}
     [:label {:for "reference"} "The ticket or order reference code:"] [:br]
     [:input (cond-> {:type "text" :required true :name "reference" :maxlength 6 :placeholder "ABCD-1"}
               ref (assoc :value ref))]
     [:br]
     [:label {:for "email"} "The email address this ticket is assigned to:"] [:br]
     [:input {:type "email" :required true :name "email" :value email}] [:br]
     [:input {:type "submit" :value "Connect"}]]])
  ([ref email]
   (connect-ticket-form nil ref email)))

(defn ticket-connected [ticket]
  [:div
   [:h2 "Ticket Connection"]
   [:p "Your ti.to ticket is already connected! You're all set!"]
   [:p "Your ticket reference is " [:strong (:tito.ticket/reference ticket)] "."]])
