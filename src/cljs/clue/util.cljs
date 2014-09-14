(ns clue.util
  (:require
   [goog.Uri :as uri]
   [goog.net.XhrIo :as xhr]))

(defn console-log [obj]
  (.log js/console (pr-str obj)))

(defn console-log-raw [msg]
  (.log js/console msg))

(defn ajax
  ([{:keys [url callback method content headers] :or [method "POST"]}]
     (xhr/send url
               #(callback (.. % -target))
               method content #js {"Content-Type" "application/edn"}))
  ([data-url callback] (xhr/send data-url callback)))
