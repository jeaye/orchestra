(ns orchestra.test
  (:require #?(:clj [clojure.test :refer [run-tests]]
               :cljs [cljs.test :refer-macros [run-tests]])
            orchestra.core-test
            orchestra.reload-test))

(run-tests)
