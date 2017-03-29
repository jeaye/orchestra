(defproject orchestra "0.1.0"
  :description "Complete clojure.spec instrumentation"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]]
  :global-vars {*warn-on-reflection* true}
  :profiles {:uberjar {:aot :all}})
