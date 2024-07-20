#!/bin/env bb

(require
 '[clojure.string :as str]
 '[clojure.java.io :as io]
 '[clojure.core :as c]
 '[clojure.pprint]
 '[clojure.java.shell :as sh]
 '[cheshire.core :as json]
 '[babashka.curl :as curl])

(def timeout 90)
(def service-name "compass")
(def gh-repo "https://github.com/GaiwanTeam/compass")
(def health-check-url "http://localhost:8080/health")
(def app-dir "/home/compass/app")
(def prep-cmd "true")
(def discord-webhook
  (when (.exists (io/file "/home/compass/config.edn"))
    (:discord/deployment-webhook (read-string (slurp "/home/compass/config.edn")))))

(let [[from to branch] (str/split (slurp *in*) #" ")]
  (def previous-git-sha from)
  (def git-sha to)
  (def git-branch branch))

(defn process-builder [args]
  (.inheritIO (ProcessBuilder. args)))

(defn color
  ([c1 c2 s]
   (str "\u001B[" c1 ";" c2 "m" s "\u001B[0m"))
  ([c s]
   (str "\u001B[" c "m" s "\u001B[0m")))

(defn timestamp []
  (color 90 (str "(" (java.time.LocalTime/now) ")")))

(defn short-sha [sha]
  (subs sha 0 8))

(defonce procs (atom #{}))

(defn exit! [code]
  (run! #(.destroyForcibly %) @procs)
  (System/exit code))

(defn sh [& args]
  (let [[opts args] (if (map? (last args))
                      [(last args) (butlast args)]
                      [{} args])
        dir (:dir opts)]
    (println
     (color 94
            (str "$ "
                 (str/join " " (map #(if (str/includes? % " ") (pr-str %) %) args))
                 (if dir (str " (in " dir ")") "")))
     (timestamp))
    (let [proc (-> (process-builder (into ["env" "-u" "GIT_QUARANTINE_PATH"] args))
                   (cond-> dir
                     (.directory (io/file dir)))
                   .start)]
      (swap! procs conj proc)
      (= 0 (.waitFor proc)))))

(defn hostname []
  (str/trim (:out (shell/sh "hostname"))))

(defn notify-discord [msg]
  (when discord-webhook
    (curl/post discord-webhook
               {:headers {"Content-Type" "application/json"}
                :body (json/generate-string {:username "compass.heartofclojure.eu"
                                             :content msg})})))

(defn fatal [& msg]
  (println (color 97 101 (str "FATAL:     " (str/join " " msg) "       ")) (timestamp))
  #_(notify-discord (str "FATAL: " (str/join " " msg)))
  (exit! -1))

(defn header [& title]
  (println)
  (println (str (color 93 "-=≡≣    ") (color 33 (str/join " " title)) (color 93 "    ≣≡=-"))
           (timestamp))
  (println)
  #_(notify-discord (str/join " " title)))

(defn health-check! []
  (let [start (System/currentTimeMillis)]
    (loop []
      (let [time-passed (/ (- (System/currentTimeMillis) start) 1000)]
        (if (try
              (let [status (:status (curl/get health-check-url))]
                (if (= 200 status)
                  true
                  (println status (int (- timeout time-passed)) (timestamp))))
              (catch Exception e
                false))
          true
          (if (< timeout time-passed)
            (do
              (println)
              false)
            (do
              (Thread/sleep 1000)
              (recur))))))))

(defn systemctl [cmd]
  (sh "sudo" "/bin/systemctl" cmd service-name))

(defn main []
  (println (color 32 " °º¤ø,¸¸,ø¤º°`°º¤ø,¸,ø¤°º¤ø,¸¸,ø¤º°`°º¤ø,¸¸¸,ø¤º°`°º¤ø,¸,ø¤°º¤ø,¸¸,ø¤º°`°º¤ø,¸ "))
  (println (color 33 "                     D E P L O Y I N G    C O M P A S S                        "))
  (println (color 32 " °º¤ø,¸¸,ø¤º°`°º¤ø,¸,ø¤°º¤ø,¸¸,ø¤º°`°º¤ø,¸¸¸,ø¤º°`°º¤ø,¸,ø¤°º¤ø,¸¸,ø¤º°`°º¤ø,¸ "))

  (if (not (re-find #"refs/heads/(master|main)" git-branch))
    (fatal "Received branch was" git-branch ". Push to `main` to deploy.")
    (let [sha          (short-sha git-sha)
          sha-dir      (str app-dir "/" sha)
          current-dir  (str app-dir "/current")
          previous-dir (str app-dir "/previous")
          old-link     (str/trim (:out (sh/sh "readlink" "-f" current-dir)))]
      (header "Deploying" sha "to" sha-dir)
      (fs/create-dirs sha-dir)
      (if-not (sh "env" "-u" "GIT_QUARANTINE_PATH"
                  "git" "-c" "advice.detachedHead=false"
                  (str "--work-tree=" sha-dir)
                  "checkout" "--force" git-sha)
        (fatal "Git checkout failed")
        (do
          (header "Running" prep-cmd)
          (if-not (sh "env" "-u" "GIT_QUARANTINE_PATH" prep-cmd {:dir sha-dir})
            (fatal "Prep command" prep-cmd "failed")
            (do
              (header "Updating symlink" )
              (sh "ln" "-sf" old-link previous-dir)
              (if-not (sh "ln" "-sfn" sha-dir current-dir)
                (do
                  (println (color 33 "Updating symlink failed, reverting"))
                  (sh "ln" "-sf" old-link current-dir)
                  (fatal "Updating symlink failed"))
                (do
                  (header "Restarting app")
                  (if-not (systemctl "restart")
                    (do
                      (println (color 33 "Service restart failed, reverting"))
                      (sh "ln" "-sf" old-link current-dir)
                      (systemctl "restart")
                      (fatal "Service restart failed"))
                    (do
                      (header "Doing health check at" health-check-url)
                      (future
                        (sh "journalctl" "-q" "-f" "-u" service-name))
                      (if-not (health-check!)
                        (let [log-lines (:out (sh/sh "journalctl" "-u" "compass" "-n" "10"))]
                          (println (color 33 "Health check failed, reverting"))
                          (sh "ln" "-sf" old-link current-dir)
                          (systemctl "restart")
                          (notify-discord
                           (str "> __**Deployment health check failed, reverted.**__\n"
                                "> ```\n"
                                (->> (str/split log-lines #"\R")
                                     (map #(str "> " %))
                                     (str/join "\n"))
                                "> ```"))
                          (fatal "Health check failed"))
                        (let [git-interval (str previous-git-sha ".." git-sha)]
                          (notify-discord
                           (str "> __**Deployed [" git-interval "](" gh-repo "/compare/" git-interval ")**__\n"
                                "> ```\n"
                                (->> (str/split (:out (sh/sh "git" "shortlog" git-interval)) #"\R")
                                     (map #(str "> " %))
                                     (str/join "\n"))
                                "> ```"
                                ))
                          (header "SUCCESSFULLY DEPLOYED" sha)
                          (exit! 0))))))))))))))

(main)

(exit! 1)

;; Local Variables:
;; mode:clojure
;; End:
