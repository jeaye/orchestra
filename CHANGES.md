## 2019.02.06-1

- Add ClojureCLR support ([#37](https://github.com/jeaye/orchestra/pull/37))
- Conform `:ret` to `:fn` ([#40](https://github.com/jeaye/orchestra/pull/40))
- Add `:orchestra.spec/var` to exception info ([#38](https://github.com/jeaye/orchestra/pull/38))
- Fix instrument/unstrument eval output ([#32](https://github.com/jeaye/orchestra/issues/32))

## 2018.12.06-2

- Caught up with the latest upstream CLJS, save for CLJS-2890 and CLJS-2891,
  since they break Expound support

## 2018.11.07-1

- Make a couple fns public, to avoid new CLJS warnings [#27](https://github.com/jeaye/orchestra/pull/27)

## 2018.09.10-1

- Allow instrumentation of a huge amount of functions [commit](https://github.com/jeaye/orchestra/commit/86f3a93918994db2ea0f90de2e767203b7b6d2c0)

## 2018.08.19-1

- Add implicit `s/spec` to non-variadic args in `defn-spec` [commit](https://github.com/jeaye/orchestra/commit/bc2561f63aace0fe6d822d8242d652254d504c49)

## 2017.11.12-1

- Add `defn-spec` macro [#7](https://github.com/jeaye/orchestra/issues/12)

## 2017.08.13

- Provide `:via` to `s/explain-data` [#7](https://github.com/jeaye/orchestra/issues/7)

## 2017.07.04

- Use date for versioning
- Add ClojureScript support [#5](https://github.com/jeaye/orchestra/issues/5)

## 0.3.0

- Update orchestra to use `clojure.spec.alpha`. Only specs registered by `clojure.spec.alpha` will be instrumented. Also added a dependency on `[org.clojure/spec.alpha "0.1.94"]`. See this [mailing list post](https://groups.google.com/forum/#!msg/clojure/10dbF7w2IQo/ec37TzP5AQAJ) for more details on the change. [#2](https://github.com/jeaye/orchestra/issues/2)
