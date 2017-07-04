(ns orchestra.core-test
  (:require #?@(:clj [[clojure.test :refer :all]
                      [clojure.spec.alpha :as s]
                      [orchestra.spec.test :as st]]

              :cljs [[cljs.test
                      :refer-macros [deftest testing is use-fixtures]]
                     [cljs.spec.alpha :as s]
                     [orchestra-cljs.spec.test :as st]])))

(defn args'
  [meow]
  true)
(s/fdef args'
        :args (s/cat :meow string?))

(deftest args
  (testing "Positive"
    (is (args' "meow")))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (args' 42)))))

(defn ret'
  [meow]
  meow)
(s/fdef ret'
        :ret integer?)

(deftest ret
  (testing "Positive"
    (is (= 42 (ret' 42))))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (ret' true)))))

(defn func'
  [meow]
  (Math/abs meow))
(s/fdef func'
        :args (s/cat :meow number?)
        :fn #(= (:ret %) (-> % :args :meow)))

(deftest func
  (testing "Positive"
    (is (= 42 (func' 42))))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (func' -42)))))

(defn full'
  [meow]
  (Math/abs meow))
(s/fdef full'
        :args (s/cat :meow number?)
        :fn #(let [meow (-> % :args :meow)
                   ret (:ret %)]
               (or (= ret meow)
                   (and (< meow 0)
                        (= (- ret) meow))))
        :ret number?)

(deftest full
  (testing "Positive"
    (is (full' 0))
    (is (full' -10))))

(defn empty-spec'
  [meow]
  (Math/abs meow))
(s/fdef empty')

(deftest empty-spec
  (testing "Positive"
    (is (empty-spec' 0))))

(defn func-no-args-spec
  [meow]
  (Math/abs meow))
(s/fdef func-no-args-spec
        :fn #(= (:ret %) (-> % :args :meow)))

(deftest func-negative
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (func-no-args-spec -42)))))

(deftest disabled
  (testing "Positive"
    (st/with-instrument-disabled
      (is (func-no-args-spec -42)))))

(defn instrument-fixture [f]
  (st/unstrument)
  (st/instrument)
  (f))
(use-fixtures :each instrument-fixture)
