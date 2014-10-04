(defproject clue "0.2.2-SNAPSHOT"
  :description "Just some Clojure glue"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring "1.2.2"]
                 [enlive "1.1.1"]
                 [clj-time "0.6.0"]
                 [com.taoensso/timbre "3.1.6"]
                 [com.novemberain/validateur "2.0.0"]
                 [environ "0.5.0"]
                 [org.clojure/clojurescript "0.0-2227"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src/clj"]

  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :compiler {:output-to "app.js"
                                   :optimizations :simple
                                   :pretty-print true}}]})
