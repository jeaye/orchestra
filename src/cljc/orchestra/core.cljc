(ns orchestra.core
  (:require [orchestra.detail :as detail]))

(defmacro defn-spec
  "Defines a function and the associated spec.

   Example usage:
   (defn-spec str->kw keyword?
     [s string?]
      (keyword s))

   Multiple arities are also supported:
   (defn-spec inc' number?
     ([a number?]
      (inc' a 1))
     ([a number?, n number?]
      (+ a n)))"
  [& args]
  ; This is a hack to determine if we're running this macro for Clojure or
  ; ClojureScript. There doesn't seem to be an official way to check this.
  (binding [detail/*cljs?* (-> &env :ns some?)]
    (apply detail/defn-spec-helper args)))
