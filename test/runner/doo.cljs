(ns runner.doo
  (:require [doo.runner :refer-macros [doo-tests]]
            orchestra-cljs.core-test
            orchestra-cljs.reload-test))

(doo-tests 'orchestra-cljs.core-test
           'orchestra-cljs.reload-test)
