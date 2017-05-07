(defproject orchestra "0.3.0"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16" :scope "provided"]
                 [org.clojure/clojurescript "1.9.521" :scope "provided"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.6"]]
  :global-vars {*warn-on-reflection* true}
  :hooks [leiningen.cljsbuild]
  :profiles {:clj {:dependencies [[org.clojure/spec.alpha "0.1.108"]]
                   :source-paths ["src/clj/" "test/"]}
             :cljs {:dependencies []}}
  :cljsbuild {:builds
              ; TODO: Clean this up with a for
              {:none {:source-paths ["src/cljs" "test"]
                      :compiler
                      {:asset-path "target/none"
                       :main orchestra.test
                       :optimizations :none
                       :output-dir "target/none"
                       :output-to "target/none.js"}}
               :advanced {:source-paths ["src/cljs" "test"]
                          :compiler
                          {:asset-path "target/advanced"
                           :main orchestra.test
                           :optimizations :advanced
                           :output-dir "target/advanced"
                           :output-to "target/advanced.js"
                           :pretty-print true}} }}
  :deploy-repositories [["releases" :clojars]])
