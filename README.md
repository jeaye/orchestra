[![Build Status](https://travis-ci.org/jeaye/orchestra.svg?branch=master)](https://travis-ci.org/jeaye/orchestra) [![Clojars Project](https://img.shields.io/clojars/v/orchestra.svg)](https://clojars.org/orchestra)
# Orchestra : complete instrumentation for clojure.spec
Orchestra is a Clojure(Script) library made as a drop-in replacement for
[clojure.spec.test.alpha](https://clojure.org/guides/spec), which provides custom
instrumentation that validates all aspects of function specs. By default,
clojure.spec will only instrument `:args`. This leaves out `:ret` and `:fn`
from automatic validation; Orchestra checks all of them for you.

## Usage
Leiningen dependency:

```clojure
;; Clojure requirements
;;    org.clojure/clojure >= 1.9.0
;;    org.clojure/spec.alpha >= 0.1.108
;;
;; ClojureScript requirements
;;    org.clojure/clojurescript >= 1.9.671
[orchestra "2018.12.06-2"]
```

Just replace your `ns` and `require` forms to reference `orchestra.spec.test`
instead of `clojure.spec.test.alpha`. No further code changes required!

```clojure
;; Before
(ns kitty-ninja
  (:require [clojure.spec.test.alpha :as st]))

;; Clojure: After
(ns kitty-ninja
  (:require [orchestra.spec.test :as st]))

;; ClojureScript: After
(ns kitty-ninja
  (:require [orchestra-cljs.spec.test :as st]))
```

Just as with vanilla Clojure, begin your instrumentation by calling:

```clojure
; Call after defining all of your specs
(st/instrument)
```

## What it does
If you're not familiar with Clojure's instrumentation, it's worth reading the
official [spec
guide](https://clojure.org/guides/spec#_instrumentation_and_testing). In short,
after calling `orchestra.spec.test/instrument`, every call to a function which
you've spec'd will have its arguments, return value, and `:fn` spec validated,
based on the specs you've provided.

This magic is possible by rebinding the var, to which your spec'd functions are
bound, with a different function which first checks all arguments, then calls
the original function, then checks the `:ret` and `:fn` specs, if they're
present.

## When to use it
I highly recommend having this **always on** during development and testing. You
may have systems tests, rather than unit tests, and this can help verify that
your data stays in the exact shape you intended.

## defn-spec
Orchestra also ships with a `defn-spec` macro for defining both functions and
their specs together in a way which encourages having more specs. You can use it
like this:

```clojure
; Clojure
(ns kitty-ninja
  (:require [orchestra.core :refer [defn-spec]]))

; ClojureScript
(ns kitty-ninja
  (:require [orchestra.core :refer-macros [defn-spec]]))

; The return spec comes after the fn name.
(defn-spec my-inc integer?
  [a integer?] ; Each argument is followed by its spec.
  (+ a 1))

(defn-spec my-add integer?
  [a integer?, b integer?] ; Commas can help visually group things.
  (+ a b))

; Doc strings work as expected.
(defn-spec my-add integer?
  "Returns the sum of `a` and `b`."
  [a integer?, b integer?]
  (+ a b))

; If a certain element doesn't have a spec, use any?
(defn-spec get-meow any?
  [meow-map (s/map-of keyword? any?)]
  (:meow meow-map))

; :fn specs can be specified using the fn's meta map.
(defn-spec my-abs number?
  {:fn #(= (:ret %) (-> % :args :n))}
  [n number?]
  (Math/abs n))

; Destructuring works nicely.
(defn-spec add-a-b number?
  [{:keys [a b]} (s/map-of keyword? number?)]
  (+ a b))

; Multiple arities are supported.
(defn-spec sum number?
  ([a number?]
   a)
  ; Varargs are also supported.
  ([a number?, b number?, & args (s/* number?)]
   (apply + a b args)))
```

### A note on defn-spec with multiple arities
Since defn-spec allows for multiple arities, each one with arbitrary specs, some
special handling needs to be done for handling how args are validated against
the right arity. For the most part, this is done entirely behind the scenes. The
one place it slips through is in `:fn` validation for multi-arity functions. In
this case, spec conforming will slightly change the input to the `:fn`
validator and that needs to be handled. Here's an example.

```clojure
; A multi-arity function like this:
(defn-spec arities number?
  ([a number?]
   (inc a))
  ([a number?, b number?]
   (+ a b))
  ([a string?, b boolean?, c map?]
   0))

; Has an automatically-generated function spec of this:
{:args (s/or :arity-1 (s/cat :a number?)
             :arity-2 (s/cat :a number? :b number?)
             :arity-3 (s/cat :a string? :b boolean? :c map?))
 :ret number?}

; If we call (arities 2 2) then then :fn validator gets this:
{:ret 4, :args [:arity-2 {:a 2, :b 2}]}

; If we call (arities "" false {}) then the :fn validator gets this:
{:ret 0, :args [:arity-3 {:a "", :b false, :c {}}]}
```

So, the `:fn` validator needs to take note of possible arities it's handling and
it can, for example, do a `case` on that value to validate differently for each
arity. Or just use `(-> % :args second)` to ignore it and get to the args.

## License
Distributed under the Eclipse Public License version 1.0, just like Clojure.
