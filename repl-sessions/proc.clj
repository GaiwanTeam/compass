(ns repl-sessions.proc
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn process-builder [args]
  (.inheritIO (java.lang.ProcessBuilder. args)))


(defonce procs (atom #{}))


(defn sh [& args]
  (let [[opts args] (if (map? (last args))
                      [(last args) (butlast args)]
                      [{} args])
        dir (:dir opts)]
    (println
     (str "$ "
          (str/join " " (map #(if (str/includes? % " ") (pr-str %) %) args))
          (if dir (str " (in " dir ")") ""))
     )
    (let [proc (-> (process-builder (into ["env" "-u" "GIT_QUARANTINE_PATH"] args))
                   (cond-> dir
                     (.directory (io/file dir)))
                   .start)]
      (swap! procs conj proc)
      (= 0 (.waitFor proc)))))

(sh "echo" "1")

(future
  (sh
   "journalctl" "-f"))

(run! #(.destroyForcibly %) @procs)
