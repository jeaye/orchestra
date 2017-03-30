(ns orchestra.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [orchestra.spec.test :refer :all]))

(defn instrument-fixture [f]
  (unstrument)
  (instrument)
  (f))
(use-fixtures :each instrument-fixture)

(defn args'
  [meow]
  true)
(s/fdef args'
        :args (s/cat :meow string?))

(deftest args
  (testing "Positive"
    (is (args' "meow")))
  (testing "Negative"
    (is (thrown? RuntimeException (args' 42)))))
