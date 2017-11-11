(ns orchestra.core-test
  (:require #?@(:clj [[clojure.test :refer :all]
                      [clojure.spec.alpha :as s]
                      [orchestra.spec.test :as st]
                      [orchestra.core :refer [defn-spec]]]

              :cljs [[cljs.test
                      :refer-macros [deftest testing is use-fixtures]]
                     [cljs.spec.alpha :as s]
                     [orchestra-cljs.spec.test :as st]
                     [orchestra.core :refer-macros [defn-spec]]])))

(defn-spec args' true?
  [meow string?]
  true)

(deftest args
  (testing "Positive"
    (is (args' "meow")))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (args' 42)))))

(defn-spec ret' integer?
  [meow any?]
  meow)

(deftest ret
  (testing "Positive"
    (is (= 42 (ret' 42))))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (ret' true)))))

(defn-spec func' number?
  {:fn #(= (:ret %) (-> % :args second :meow))}
  [meow number?]
  (Math/abs meow))

(deftest func
  (testing "Positive"
    (is (= 42 (func' 42))))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (func' -42)))))

(defn-spec full' number?
  {:fn #(let [meow (-> % :args second :meow)
              ret (:ret %)]
          (or (= ret meow)
              (and (< meow 0)
                   (= (- ret) meow))))}
  [meow number?]
  (Math/abs meow))

(deftest full
  (testing "Positive"
    (is (full' 0))
    (is (full' -10))))

(defn-spec destruct-map' number?
  [{:keys [a b]} (s/map-of keyword? number?)]
  (+ a b))

(deftest destruct-map
  (testing "Positive"
    (is (= 42 (destruct-map' {:a 30 :b 12}))))
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (destruct-map' nil)))))

(defn-spec doc-string' nil?
  "Doc strings also work just fine."
  []
  nil)

(deftest doc-string
  (testing "Invocation"
    (is (nil? (doc-string'))))
  (testing "Meta"
    (is (= "Doc strings also work just fine."
           (-> #'doc-string' meta :doc)))))

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
        :fn #(= (:ret %) (-> % :args second :meow)))

(deftest func-negative
  (testing "Negative"
    (is (thrown? #?(:clj RuntimeException :cljs :default)
                 (func-no-args-spec -42)))))

(deftest disabled
  (testing "Positive"
    (st/with-instrument-disabled
      (is (func-no-args-spec -42)))))

(defn-spec instrument-fixture any?
  [f fn?]
  (st/unstrument)
  (st/instrument)
  (f))
(use-fixtures :each instrument-fixture)
