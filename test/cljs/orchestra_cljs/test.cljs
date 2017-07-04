(ns orchestra-cljs.test
  (:require [doo.runner :refer-macros [doo-tests]]
            orchestra.core-test))

(doo-tests 'orchestra.core-test)
