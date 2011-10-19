(defproject org.github.pistacchio.deviantchecker "0.9.0"
  :description "A one page application for keeping track of changes on deviantart.com gallieries"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [enlive  "1.0.0"]
                 [compojure "0.6.4"]
                 [ring "1.0.0-beta2"]]
  :dev-dependencies [[lein-ring "0.4.6"]]
  :ring {:handler org.github.pistacchio.deviantchecker.core/app}
  :main org.github.pistacchio.deviantchecker.core)
