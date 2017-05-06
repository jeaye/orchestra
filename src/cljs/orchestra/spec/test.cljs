;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cljs.spec.test.alpha
  (:require-macros [cljs.spec.test.alpha :as m :refer [with-instrument-disabled]])
  (:require
    [goog.object :as gobj]
    [goog.userAgent.product :as product]
    [clojure.string :as string]
    [cljs.stacktrace :as st]
    [cljs.pprint :as pp]
    [cljs.spec.alpha :as s]
    [cljs.spec.gen.alpha :as gen]
    [clojure.test.check :as stc]
    [clojure.test.check.properties]))

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

(defn ->sym
  [x]
  (@#'s/->sym x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; instrument ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defn- fn-spec?
  "Fn-spec must include at least :args or :ret specs."
  [m]
  (or (:args m) (:ret m)))

;; wrap spec/explain-data until specs always return nil for ok data
(defn- explain-data*
  [spec v]
  (when-not (s/valid? spec v nil)
    (s/explain-data spec v)))

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

;; TODO: check ::caller result in other browsers - David

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

(defn- no-fspec
  [v spec]
  (ex-info (str "Fn at " v " is not spec'ed.")
           {:var v :spec spec ::s/failure :no-fspec}))

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

(defn- instrument-1*
  [s v opts]
  (let [spec (s/get-spec v)
        {:keys [raw wrapped]} (get @instrumented-vars v)
        current @v
        to-wrap (if (= wrapped current) raw current)
        ospec (or (instrument-choose-spec spec s opts)
                  (throw (no-fspec v spec)))
        ofn (instrument-choose-fn to-wrap ospec s opts)
        checked (spec-checking-fn v ofn ospec)]
    (swap! instrumented-vars assoc v {:raw to-wrap :wrapped checked})
    checked))

(defn- unstrument-1*
  [s v]
  (when v
    (when-let [{:keys [raw wrapped]} (get @instrumented-vars v)]
      (swap! instrumented-vars dissoc v)
      (let [current @v]
        (when (= wrapped current)
          raw)))))

(defn- fn-spec-name?
  [s]
  (symbol? s))

(defn- collectionize
  [x]
  (if (symbol? x)
    (list x)
    x))

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
