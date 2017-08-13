;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns orchestra.spec.test
  (:refer-clojure :exclude [test])
  (:require
   [clojure.pprint :as pp]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.string :as str]))

(defn ->sym
  [x]
  (@#'s/->sym x))

(defn- collectionize
  [x]
  (if (symbol? x)
    (list x)
    x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; instrument ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private ^:dynamic *instrument-enabled*
  "if false, instrumented fns call straight through"
  true)

(defn- no-fspec
  [v spec]
  (ex-info (str "Fn at " v " is not spec'ed.")
           {:var v :spec spec ::s/failure :no-fspec}))

(defn- no-args-spec
  [v spec]
  (ex-info (str "Args for " v " are not spec'ed.")
           {:var v :spec spec ::s/failure :no-args-spec}))

(defmacro with-instrument-disabled
  "Disables instrument's checking of calls, within a scope."
  [& body]
  `(binding [*instrument-enabled* nil]
     ~@body))

(defn- interpret-stack-trace-element
  "Given the vector-of-syms form of a stacktrace element produced
by e.g. Throwable->map, returns a map form that adds some keys
guessing the original Clojure names. Returns a map with

  :class         class name symbol from stack trace
  :method        method symbol from stack trace
  :file          filename from stack trace
  :line          line number from stack trace
  :var-scope     optional Clojure var symbol scoping fn def
  :local-fn      optional local Clojure symbol scoping fn def

For non-Clojure fns, :scope and :local-fn will be absent."
  [[cls method file line]]
  (let [clojure? (contains? '#{invoke invokeStatic} method)
        demunge #(clojure.lang.Compiler/demunge %)
        degensym #(str/replace % #"--.*" "")
        [ns-sym name-sym local] (when clojure?
                                  (->> (str/split (str cls) #"\$" 3)
                                       (map demunge)))]
    (merge {:file file
            :line line
            :method method
            :class cls}
           (when (and ns-sym name-sym)
             {:var-scope (symbol ns-sym name-sym)})
           (when local
             {:local-fn (symbol (degensym local))}))))

(defn- stacktrace-relevant-to-instrument
  "Takes a coll of stack trace elements (as returned by
StackTraceElement->vec) and returns a coll of maps as per
interpret-stack-trace-element that are relevant to a
failure in instrument."
  [elems]
  (let [plumbing? (fn [{:keys [var-scope]}]
                    (contains? '#{orchestra.spec.test/spec-checking-fn} var-scope))]
    (sequence (comp (map StackTraceElement->vec)
                    (map interpret-stack-trace-element)
                    (filter :var-scope)
                    (drop-while plumbing?))
              elems)))

(defn- spec-checking-fn
  [v f raw-fn-spec]
  (let [fn-spec (@#'s/maybe-spec raw-fn-spec)
        conform! (fn [v role spec data data-key]
                   (with-instrument-disabled
                     (let [conformed (s/conform spec data)]
                       (if (= ::s/invalid conformed)
                         (let [caller (->> (.getStackTrace (Thread/currentThread))
                                           stacktrace-relevant-to-instrument
                                           first)
                               via (if-some [n (#'s/spec-name spec)]
                                     [n]
                                     [])
                               ed (merge (assoc (s/explain-data* spec [role]
                                                                 via
                                                                 []
                                                                 data)
                                                data-key data
                                                ::s/failure :instrument)
                                         (when caller
                                           {::caller (dissoc caller :class :method)}))]
                           (throw (ex-info
                                    (str "Call to " v " did not conform to spec:\n"
                                         (with-out-str (s/explain-out ed)))
                                    ed)))
                         conformed))))]
    (fn
      [& args]
      (if *instrument-enabled*
        (let [cargs (when-let [spec (:args fn-spec)]
                      (conform! v :args spec args ::s/args))
              ret (.applyTo ^clojure.lang.IFn f args)]
          (when-let [spec (:ret fn-spec)]
            (conform! v :ret spec ret ::s/ret))
          (when-let [spec (:fn fn-spec)]
            (if (nil? cargs)
              (throw (no-args-spec v fn-spec))
              (conform! v :fn spec {:ret ret :args cargs} ::s/fn)))
          ret)
        (.applyTo ^clojure.lang.IFn f args)))))

(defonce ^:private instrumented-vars (atom {}))

(defn- instrument-choose-fn
  "Helper for instrument."
  [f spec sym {over :gen :keys [stub replace]}]
  (if (some #{sym} stub)
    (-> spec (s/gen over) gen/generate)
    (get replace sym f)))

(defn- instrument-choose-spec
  "Helper for instrument"
  [spec sym {overrides :spec}]
  (get overrides sym spec))

(defn- instrument-1
  [s opts]
  (when-let [v (resolve s)]
    (when-not (-> v meta :macro)
      (let [spec (s/get-spec v)
            {:keys [raw wrapped]} (get @instrumented-vars v)
            current @v
            to-wrap (if (= wrapped current) raw current)
            ospec (or (instrument-choose-spec spec s opts)
                      (throw (no-fspec v spec)))
            ofn (instrument-choose-fn to-wrap ospec s opts)
            checked (spec-checking-fn v ofn ospec)]
        (alter-var-root v (constantly checked))
        (swap! instrumented-vars assoc v {:raw to-wrap :wrapped checked})
        (->sym v)))))

(defn- unstrument-1
  [s]
  (when-let [v (resolve s)]
    (when-let [{:keys [raw wrapped]} (get @instrumented-vars v)]
      (swap! instrumented-vars dissoc v)
      (let [current @v]
        (when (= wrapped current)
          (alter-var-root v (constantly raw))
          (->sym v))))))

(defn- fn-spec-name?
  [s]
  (and (symbol? s)
       (not (some-> (resolve s) meta :macro))))

(defn instrumentable-syms
  "Given an opts map as per instrument, returns the set of syms
that can be instrumented."
  ([] (instrumentable-syms nil))
  ([opts]
     (assert (every? ident? (keys (:gen opts))) "instrument :gen expects ident keys")
     (reduce into #{} [(filter fn-spec-name? (keys (s/registry)))
                       (keys (:spec opts))
                       (:stub opts)
                       (keys (:replace opts))])))

(defn instrument
  "Instruments the vars named by sym-or-syms, a symbol or collection
of symbols, or all instrumentable vars if sym-or-syms is not
specified.

If a var has an :args fn-spec, sets the var's root binding to a
fn that checks arg conformance (throwing an exception on failure)
before delegating to the original fn.

The opts map can be used to override registered specs, and/or to
replace fn implementations entirely. Opts for symbols not included
in sym-or-syms are ignored. This facilitates sharing a common
options map across many different calls to instrument.

The opts map may have the following keys:

  :spec     a map from var-name symbols to override specs
  :stub     a set of var-name symbols to be replaced by stubs
  :gen      a map from spec names to generator overrides
  :replace  a map from var-name symbols to replacement fns

:spec overrides registered fn-specs with specs your provide. Use
:spec overrides to provide specs for libraries that do not have
them, or to constrain your own use of a fn to a subset of its
spec'ed contract.

:stub replaces a fn with a stub that checks :args, then uses the
:ret spec to generate a return value.

:gen overrides are used only for :stub generation.

:replace replaces a fn with a fn that checks args conformance, then
invokes the fn you provide, enabling arbitrary stubbing and mocking.

:spec can be used in combination with :stub or :replace.

Returns a collection of syms naming the vars instrumented."
  ([] (instrument (instrumentable-syms)))
  ([sym-or-syms] (instrument sym-or-syms nil))
  ([sym-or-syms opts]
     (locking instrumented-vars
       (into
        []
        (comp (filter (instrumentable-syms opts))
              (distinct)
              (map #(instrument-1 % opts))
              (remove nil?))
        (collectionize sym-or-syms)))))

(defn unstrument
  "Undoes instrument on the vars named by sym-or-syms, specified
as in instrument. With no args, unstruments all instrumented vars.
Returns a collection of syms naming the vars unstrumented."
  ([] (unstrument (map ->sym (keys @instrumented-vars))))
  ([sym-or-syms]
     (locking instrumented-vars
       (into
        []
        (comp (filter symbol?)
              (map unstrument-1)
              (remove nil?))
        (collectionize sym-or-syms)))))
