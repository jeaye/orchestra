(defproject orchestra "0.3.0"
  :description "Complete instrumentation for clojure.spec"
  :url "https://github.com/jeaye/orchestra"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript "1.9.521" :scope "provided"]]
  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.6"]]
  :global-vars {*warn-on-reflection* true}
  :profiles {:clj {:dependencies [[org.clojure/spec.alpha "0.1.108"]]
                   :source-paths ["src/clj/" "test"]}
             :cljs {:dependencies []}
                   :source-paths ["src/cljs/" "test"]}
  :cljsbuild {:builds
              [{:id "none"
                :compiler
                {:asset-path "target/none"
                 ;:main cljs-http.test
                 :optimizations :none
                 :output-dir "target/none"
                 :output-to "target/none.js"}}
               {:id "advanced"
                :compiler
                {:asset-path "target/advanced"
                 ;:main cljs-http.test
                 :optimizations :advanced
                 :output-dir "target/advanced"
                 :output-to "target/advanced.js"
                 :pretty-print true}}]}
  :deploy-repositories [["releases" :clojars]])
