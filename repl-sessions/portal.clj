(ns portal)

(clojure.java.shell/sh
 "/home/arne/opt/firefox-nightly/firefox"
 (portal.runtime.jvm.launcher/url @user/portal-instance)
 :env {"DISPLAY" ":0"})

(println (portal.runtime.jvm.launcher/url @user/portal-instance))

;;
(require '[portal.api :as p])

(def portal-instance (atom nil))

(defn portal
  "Open a Portal window and register a tap handler for it. The result can be
  treated like an atom."
  []
  ;; Portal doesn't recognize records as maps, make them at least datafiable
  (extend-protocol clojure.core.protocols/Datafiable
    clojure.lang.IRecord
    (datafy [r] (into {} r)))
  (let [pi (p/open @portal-instance)]
    (reset! portal-instance pi)
    (add-tap p/submit)
    pi))
