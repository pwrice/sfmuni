(defproject sfmuni "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://sfmuni.herokuapp.com"
  :license {:name "FIXME: choose"
            :url "http://example.com/FIXME"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.1"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring/ring-devel "1.1.0"]
                 [ring-basic-authentication "1.0.1"]
                 [environ "0.2.1"]
                 [com.cemerick/drawbridge "0.0.6"]
                 [org.clojure/core.logic "0.8.0-rc2"]
                 [enlive "1.0.0"]
                 [org.clojure/data.json "0.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 ]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.2.1"]]
  :hooks [environ.leiningen.hooks]
  :profiles {:production {:env {:production true}}})