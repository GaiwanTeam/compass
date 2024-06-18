(ns portal)


(clojure.java.shell/sh
 "/home/arne/opt/firefox-nightly/firefox"
 (portal.runtime.jvm.launcher/url @user/portal-instance)
 :env {"DISPLAY" ":0"})

(println (portal.runtime.jvm.launcher/url @user/portal-instance))
