(defproject orchestra "2017.07.04-1"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17" :scope "provided"]
                 [org.clojure/clojurescript "1.9.671" :scope "provided"]
                 [org.clojure/spec.alpha "0.1.123" :scope "provided"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.10"]
            [lein-doo "0.1.7"]]
  :hooks [leiningen.cljsbuild]
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clj/" "src/cljs/"]
  :cljsbuild {:test-commands {"test" ["lein" "doo" "node" "test" "once"]}
              :builds {:test
                       {:source-paths ["test/cljs/" "test/cljc/"
                                       "src/cljs/"]
                        :compiler
                        {:main orchestra-cljs.test
                         :target :nodejs
                         :optimizations :advanced
                         :pretty-print false
                         :parallel-build true
                         :output-dir "target/test"
                         :output-to "target/test.js"}}}}
  :profiles {:dev {:dependencies [[lein-doo "0.1.7"]]
                   :source-paths ["test/clj/" "test/cljc/"]}}

  :deploy-repositories [["releases" :clojars]])
