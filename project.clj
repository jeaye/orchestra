(defproject orchestra "0.3.0"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671"]
                 [org.clojure/spec.alpha "0.1.123"]
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [lein-doo "0.1.7"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.10"]
            [lein-doo "0.1.7"]]
  :hooks [leiningen.cljsbuild]
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clj/"
                 "test/clj/" "test/cljc/"]
  :cljsbuild {:test-commands {"test" ["lein" "doo" "node" "test" "once"]}
              :builds {:test {:source-paths ["src/cljs/"
                                             "test/cljs/" "test/cljc/"]
                              :compiler
                              {:main orchestra-cljs.test
                               :target :nodejs
                               :optimizations :advanced
                               :parallel-build true
                               :output-dir "target/test"
                               :output-to "target/test.js"}}}}
  :deploy-repositories [["releases" :clojars]])
