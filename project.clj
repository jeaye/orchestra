(defproject orchestra "0.3.0"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16" :scope "provided"]
                 [org.clojure/clojurescript "1.9.521" :scope "provided"]
                 [lein-doo "0.1.7"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.10"]
            [lein-doo "0.1.7"]]
  :global-vars {*warn-on-reflection* true}
  :hooks [leiningen.cljsbuild]
  :profiles {:clj {:dependencies [[org.clojure/spec.alpha "0.1.108"]]
                   :source-paths ["src/clj/" "test/"]}
             :cljs {:dependencies []}}
  :cljsbuild {:test-commands {"test" ["lein" "doo" "phantom" "test" "once"]}
              ; TODO: Clean this up with a for
              :builds {:dev {:source-paths ["src/cljs/"]
                             :figwheel true
                             :compiler
                             {:main orchestra.test
                              :optimizations :none
                              :output-to "target/dev.js"}}
                       :prod {:source-paths ["src/cljs/"]
                              :compiler
                              {:main orchestra.test
                               :optimizations :prod
                               :output-to "target/advanced.js"
                               :pretty-print true}}
                       :test {:source-paths ["src/cljs/" "test/"]
                              :compiler
                              {:main runner.doo
                               :optimizations :none
                               :output-to "target/test.js"}}}}
  :deploy-repositories [["releases" :clojars]])
