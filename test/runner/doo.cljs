(ns runner.doo
  (:require [doo.runner :refer-macros [doo-all-tests]]
            orchestra.test))

(doo-all-tests #"(orchestra)\..*-test")
