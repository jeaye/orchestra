(ns orchestra-cljs.test
  (:require [doo.runner :refer-macros [doo-tests]]
            orchestra.core-test
            orchestra.expound-test
            orchestra.many-fns))

(doo-tests 'orchestra.core-test
           'orchestra.expound-test
           'orchestra.many-fns)
