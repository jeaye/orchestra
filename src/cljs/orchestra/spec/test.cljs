;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns orchestra.spec.test
  (:require
    [cljs.analyzer :as ana]
    [cljs.analyzer.api :as ana-api]
    [clojure.string :as string]
    [cljs.spec :as s]
    [cljs.spec.impl.gen :as gen]))

(defonce ^:private instrumented-vars-macros (atom #{}))
(defonce ^:private instrumented-vars-fns (atom {}))

(def ^:private ^:dynamic *instrument-enabled*
  "if false, instrumented fns call straight through"
  true)

(defn get-host-port []
  (if (not= "browser" *target*)
    {}
    {:host (.. js/window -location -host)
     :port (.. js/window -location -port)}))

(defn get-ua-product []
  (if (not= "browser" *target*)
    (keyword *target*)
    (cond
      product/SAFARI :safari
      product/CHROME :chrome
      product/FIREFOX :firefox
      product/IE :ie)))

(defn get-env []
  {:ua-product (get-ua-product)})

(defn- collectionize
  [x]
  (if (symbol? x)
    (list x)
    x))

(defn- fn-spec-name?
  [s]
  (symbol? s))

(defn distinct-by
  ([f coll]
   (let [step (fn step [xs seen]
                (lazy-seq
                  ((fn [[x :as xs] seen]
                     (when-let [s (seq xs)]
                       (let [v (f x)]
                         (if (contains? seen v)
                           (recur (rest s) seen)
                           (cons x (step (rest s) (conj seen v)))))))
                   xs seen)))]
     (step coll #{}))))

(defn- no-fspec
  [v spec]
  (ex-info (str "Fn at " v " is not spec'ed.")
           {:var v :spec spec ::s/failure :no-fspec}))

(defmacro with-instrument-disabled
  "Disables instrument's checking of calls, within a scope."
  [& body]
  `(binding [*instrument-enabled* nil]
     ~@body))

(defn- instrument-choose-spec
  "Helper for instrument"
  [spec sym {overrides :spec}]
  (get overrides sym spec))

(defn- instrument-choose-fn
  "Helper for instrument."
  [f spec sym {over :gen :keys [stub replace]}]
  (if (some #{sym} stub)
    (-> spec (s/gen over) gen/generate)
    (get replace sym f)))

(defn- find-caller [st]
  (letfn [(search-spec-fn [frame]
            (when frame
              (let [s (:function frame)]
                (and (string? s) (not (string/blank? s))
                     (re-find #"cljs\.spec\.test\.spec_checking_fn" s)))))]
    (->> st
      (drop-while #(not (search-spec-fn %)))
      (drop-while search-spec-fn)
      first)))

(defn- spec-checking-fn
  [v f fn-spec]
  (let [fn-spec (@#'s/maybe-spec fn-spec)
        conform! (fn [v role spec data args]
                   (let [conformed (s/conform spec data)]
                     (if (= ::s/invalid conformed)
                       (let [caller (find-caller
                                      (st/parse-stacktrace
                                        (get-host-port)
                                        (.-stack (js/Error.))
                                        (get-env) nil))
                             ed (merge (assoc (s/explain-data* spec [role] [] [] data)
                                              ::s/args args
                                              ::s/failure :instrument)
                                       (when caller
                                         {::caller caller}))]
                         (throw (ex-info
                                  (str "Call to " v " did not conform to spec:\n" (with-out-str (s/explain-out ed)))
                                  ed)))
                       conformed)))]
    (doto
      (fn
        [& args]
        (if *instrument-enabled*
          (with-instrument-disabled
            (when (:args fn-spec) (conform! v :args (:args fn-spec) args args))
            (binding [*instrument-enabled* true]
              (apply f args)))
          (apply f args)))
      (gobj/extend f))))

(defn- instrument-1*
  [s v opts]
  (let [spec (s/get-spec v)
        {:keys [raw wrapped]} (get @instrumented-vars-fns v)
        current @v
        to-wrap (if (= wrapped current) raw current)
        ospec (or (instrument-choose-spec spec s opts)
                  (throw (no-fspec v spec)))
        ofn (instrument-choose-fn to-wrap ospec s opts)
        checked (spec-checking-fn v ofn ospec)]
    (swap! instrumented-vars-fns assoc v {:raw to-wrap :wrapped checked})
    checked))

(defmacro instrument-1
  [[quote s] opts]
  (when-let [v (ana-api/resolve &env s)]
    (swap! instrumented-vars-macros conj (:name v))
    `(let [checked# (instrument-1* ~s (var ~s) ~opts)]
       (when checked# (set! ~s checked#))
       '~(:name v))))

(defn- unstrument-1*
  [s v]
  (when v
    (when-let [{:keys [raw wrapped]} (get @instrumented-vars-fns v)]
      (swap! instrumented-vars-fns dissoc v)
      (let [current @v]
        (when (= wrapped current)
          raw)))))

(defmacro unstrument-1
  [[quote s]]
  (when-let [v (ana-api/resolve &env s)]
    (when (@instrumented-vars-macros (:name v))
      (swap! instrumented-vars-macros disj (:name v))
      `(let [raw# (unstrument-1* ~s (var ~s))]
         (when raw# (set! ~s raw#))
         '~(:name v)))))

(defn- sym-or-syms->syms [sym-or-syms]
  (into []
        (mapcat
          (fn [sym]
            (if (and (string/includes? (str sym) ".")
                     (ana-api/find-ns sym))
              (->> (vals (ana-api/ns-interns sym))
                   (filter #(not (:macro %)))
                   (map :name)
                   (map
                     (fn [name-sym]
                       (symbol (name sym) (name name-sym)))))
              [sym])))
        (collectionize sym-or-syms)))

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

(defmacro instrument
  "Instruments the vars named by sym-or-syms, a symbol or collection
   of symbols, or all instrumentable vars if sym-or-syms is not
   specified. If a symbol identifies a namespace then all symbols in that
   namespace will be enumerated.
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
  ([]
   `(instrument '[~@(#?(:clj  s/speced-vars
                        :cljs cljs.spec$macros/speced-vars))]))
  ([xs]
   `(instrument ~xs nil))
  ([sym-or-syms opts]
   (let [syms (sym-or-syms->syms (eval sym-or-syms))
         opts-sym (gensym "opts")]
     `(let [~opts-sym ~opts]
        (reduce
          (fn [ret# [_# f#]]
            (let [sym# (f#)]
              (cond-> ret# sym# (conj sym#))))
          []
          (->> (zipmap '~syms
                       [~@(map
                            (fn [sym]
                              `(fn [] (instrument-1 '~sym ~opts-sym)))
                            syms)])
               (filter #((instrumentable-syms ~opts-sym) (first %)))
               (distinct-by first)))))))

(defmacro unstrument
  "Undoes instrument on the vars named by sym-or-syms, specified
   as in instrument. With no args, unstruments all instrumented vars.
   Returns a collection of syms naming the vars unstrumented."
  ([]
   `(unstrument '[~@(deref instrumented-vars-macros)]))
  ([sym-or-syms]
   (let [syms (sym-or-syms->syms (eval sym-or-syms))]
     `(reduce
        (fn [ret# f#]
          (let [sym# (f#)]
            (cond-> ret# sym# (conj sym#))))
        []
        [~@(->> syms
                (map
                  (fn [sym]
                    (when (symbol? sym)
                      `(fn []
                         (unstrument-1 '~sym)))))
                (remove nil?))]))))
