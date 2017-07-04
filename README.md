[![Build Status](https://travis-ci.org/jeaye/orchestra.svg?branch=master)](https://travis-ci.org/jeaye/orchestra) [![codecov](https://codecov.io/gh/jeaye/orchestra/branch/master/graph/badge.svg)](https://codecov.io/gh/jeaye/orchestra) [![Clojars Project](https://img.shields.io/clojars/v/orchestra.svg)](https://clojars.org/orchestra)
# Orchestra : complete instrumentation for clojure.spec
Orchestra is a Clojure(Script) library made as a drop-in replacement for
[clojure.spec.test.alpha](https://clojure.org/guides/spec), which provides custom
instrumentation that validates all aspects of function specs. By default,
clojure.spec will only instrument `:args`.  This leaves out `:ret` and `:fn`
from automatic validation; Orchestra checks all of them for you.

## Usage
Leiningen dependency:

```clojure
;; Depending on your Clojure version, choose one of the following. All of them
;; provide the same API but follow different internal spec changes.

;; Clojure requirements
;;    org.clojure/clojure >= 1.9.0-alpha16
;;    org.clojure/spec.alpha >= 0.1.108
;;
;; ClojureScript requirements
;;    org.clojure/clojurescript >= 1.9.671
[orchestra "2017.07.04"]

;; Requires 1.9.0 >= Clojure < 1.9.0-alpha16
;; Does not support ClojureScript
[orchestra "0.2.0"]
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

## Donate
Feel free to shoot Bitcoins my way: **123NMGCvRZLfQJwk2AhsLMLSpCCJhCRoz6**

For more information regarding how I use donations, see
[here](http://jeaye.com/donate/).

## License
Distributed under the Eclipse Public License version 1.0, just like Clojure.
