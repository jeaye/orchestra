(ns orchestra.make-fns
  (:require [clojure.spec.alpha :as s]))

#?(:clj (defmacro make-fns [fn-count]
          `(do
             ~@(for [i (range fn-count)]
                 (let [fn-name (symbol (str "fn-" i))]
                   `(do
                      (defn ~fn-name []
                        (str ~fn-name))
                      (s/fdef ~fn-name)))))))
