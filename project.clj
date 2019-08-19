(defproject cq "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :min-lein-version "2.3.4"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; nice utilities
                 [clj-time "0.12.0"]
                 ;; command line option processing
                 [org.clojure/tools.cli "0.2.2"]
                 ;; logging with log4j
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/tools.logging "0.2.6"]
                 [robert/hooke "1.3.0"]
                 ;; JSON parsing library
                 [cheshire "5.6.3"]
                 ;; web server
                 [compojure "1.5.1"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring.middleware.jsonp "0.1.6"]]
  :main cq.main)
