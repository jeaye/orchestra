(ns orchestra-cljs.spec.test
  (:require [cljs.spec.test.alpha :as st]))

(defmacro instrument [& args]
  `(st/instrument ~@args))

(defmacro unstrument [& args]
  `(st/unstrument ~@args))
