# Orchestra : complete instrumentation for clojure.spec
Orchestra is a Clojure library made as a drop-in replacement for
[clojure.spec.test](https://clojure.org/guides/spec), which provides custom
instrumentation that validates all aspects of function specs. By default,
clojure.spec will only instrument `:args`.  This leaves out `:ret` and `:fn`
from automatic validation; Orchestra checks all of them for you.

## Usage
Leiningen dependency:

```clojure
;; Requires Clojure >= 1.9.0
[orchestra "0.1.0-SNAPSHOT"]
```

Just replace your `ns` and `require` forms to reference `orchestra.spec.test`
instead of `clojure.spec.test`. No further code changes required!

```clojure
;; Before
(ns kitty-ninja
  (:require [clojure.spec.test :as st]))

;; After
(ns kitty-ninja
  (:require [orchestra.spec.test :as st]))
```

## What it does
If you're not familiar with Clojure's instrumentation, it's worth reading the
official [spec
guide](https://clojure.org/guides/spec#_instrumentation_and_testing). In short,
after calling `orchestra.spec.test/instrument`, every function in your codebase,
which you've spec'd, will automatically be checked at every single invocation.
Every argument you spec'd will be validated, along with return value specs, and
the more powerful `:fn` specs, which operate on both the function's arguments
and the generated return value.

This magic is possible by rebinding the var, to which your spec'd functions are
bound, with a different function which first checks all arguments, then calls
the original function, then checks the output and `:fn` spec, if those specs are
present.

## Donate
Feel free to shoot Bitcoins my way: **123NMGCvRZLfQJwk2AhsLMLSpCCJhCRoz6**

For more information regarding how I use donations, see
[here](http://jeaye.com/donate/).

## License
Distributed under the Eclipse Public License version 1.0, just like Clojure.
