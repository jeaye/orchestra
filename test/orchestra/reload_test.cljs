(ns orchestra.reload-test
  (:require ;#?@(:clj [[clojure.test :refer :all]
            ;          [clojure.spec.alpha :as s]
            ;          [orchestra.spec.test :refer :all]]
            ;   :cljs [[cljs.test :refer-macros [deftest testing is use-fixtures]]
            ;          [cljs.spec :as s]
            ;          [orchestra.spec.test :refer-macros [instrument unstrument
            ;                                              with-instrument-disabled]]])
            [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [cljs.spec.alpha :as s]
            ;[orchestra.spec.test :refer [instrument unstrument
            ;                             with-instrument-disabled]]
            ))

;(deftest in-place-reload
;  (testing "Positive"
;    (dotimes [_ 5]
;      (require 'orchestra.spec.test :reload-all))))
