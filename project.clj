(defproject orchestra "0.3.0"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16" :scope "provided"]
                 [org.clojure/clojurescript "1.9.521" :scope "provided"]
                 [org.clojure/spec.alpha "0.1.108" :scope "provided"]
                 [lein-doo "0.1.7"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.10"]
            [lein-doo "0.1.7"]]
  :hooks [leiningen.cljsbuild]
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clj/" "test/"]
  :cljsbuild {:test-commands {"test" ["lein" "doo" "phantom" "test" "once"]}
              ; TODO: Test also with optimizations
              :builds {:test {:source-paths ["src/cljs/" "test/"]
                              :compiler
                              {:main runner.doo
                               :optimizations :none
                               :output-dir "target/test"
                               :output-to "target/test.js"}}}}
  :deploy-repositories [["releases" :clojars]])
