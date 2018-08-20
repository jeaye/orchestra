(defproject orchestra "2018.08.19-1"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.339" :scope "provided"]
                 [org.clojure/spec.alpha "0.2.168" :scope "provided"]]
  :plugins [[lein-cloverage "1.0.11"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]
            [lein-doo "0.1.10"]
            [com.jakemccrary/lein-test-refresh "0.23.0"]]
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clj/" "src/cljc/" "src/cljs/"]
  :cljsbuild {:test-commands {"test" ["lein" "doo" "node" "app" "once"]}
              :builds {:app
                       {:source-paths ["src/cljs/"]
                        :compiler
                        {:optimizations :advanced
                         :pretty-print false
                         :parallel-build true
                         :output-dir "target/test"
                         :output-to "target/test.js"}}}}
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[lein-doo "0.1.10"]]
                   :source-paths ["test/clj/" "test/cljc/"]
                   :cljsbuild {:builds {:app
                                        {:source-paths ["test/cljs/" "test/cljc/"]
                                         :compiler
                                         {:main orchestra-cljs.test
                                          :target :nodejs}}}}}})
