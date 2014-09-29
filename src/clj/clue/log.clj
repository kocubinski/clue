(ns clue.log
  (:use [environ.core :only [env]])
  (:require [taoensso.timbre :as log]
            [taoensso.timbre.appenders.rotor :as rotor]))

(defn project-dir []
  (str
   (.getPath (clojure.java.io/resource ""))
   ".."))

(defonce log-config
  (log/set-config!
   [:appenders :rotor]
   {:min-level :debug
    :enabled? true
    :async? false ; should be always false for rotor
    :max-message-per-msecs nil
    :fn rotor/appender-fn}))

(defonce rotor-config
  (log/set-config!
    [:shared-appender-config :rotor]
    {:path (str (project-dir) "/log/log") :max-size 1000000 :backlog 10}))













