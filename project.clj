(defproject orchestra "0.2.0-SNAPSHOT"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/spec.alpha "0.1.94"]]
  :plugins [[lein-cloverage "1.0.9"]]
  :global-vars {*warn-on-reflection* true}
  :profiles {:uberjar {:aot :all}})
