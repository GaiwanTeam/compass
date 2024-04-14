(ns asami)

(require '[asami.core :as d])

(def db-uri "asami:local://compass.db")
(d/create-database db-uri)

;; Create a connection to the database
(def conn (d/connect db-uri))

;; Data can be loaded into a database either as objects, or "add" statements:
(def first-movies [{:movie/title "Explorers"
                    :movie/genre "adventure/comedy/family"
                    :movie/release-year 1985}
                   {:movie/title "Demolition Man"
                    :movie/genre "action/sci-fi/thriller"
                    :movie/release-year 1993}
                   {:movie/title "Johnny Mnemonic"
                    :movie/genre "cyber-punk/action"
                    :movie/release-year 1995}
                   {:movie/title "Toy Story"
                    :movie/genre "animation/adventure"
                    :movie/release-year 1995}])

@(d/transact conn {:tx-data first-movies})

(class
 (d/entity (d/db conn)
           (ffirst
            (d/q '[:find ?e
                   :where
                   [?e :movie/title]]
                 (d/db conn)))))
