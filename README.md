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

## Donate
Feel free to shoot Bitcoins my way: **123NMGCvRZLfQJwk2AhsLMLSpCCJhCRoz6**

For more information regarding how I use donations, see
[here](http://jeaye.com/donate/).

## License
Distributed under the Eclipse Public License version 1.0, just like Clojure.
