(ns orchestra-cljs.spec.test
  (:require-macros [orchestra-cljs.spec.test :refer [instrument unstrument]])
  (:require [cljs.stacktrace :as stack]
            [cljs.spec.alpha :as s]
            [cljs.spec.test.alpha :refer-macros [with-instrument-disabled setup-static-dispatches] :as st]))

(defn no-args-spec
  [v spec]
  (ex-info (str "Args for " v " are not spec'ed.")
           {:var v :spec spec ::s/failure :no-args-spec}))

(defn spec-checking-fn
  [v f raw-fn-spec]
  (let [fn-spec (@#'s/maybe-spec raw-fn-spec)
        args-spec (:args fn-spec)
        conform! (fn [v role spec data data-key]
                   (with-instrument-disabled
                     (let [conformed (s/conform spec data)]
                       (if (= ::s/invalid conformed)
                         (let [caller (#'st/find-caller (stack/parse-stacktrace (st/get-host-port)
                                                                                (.-stack (js/Error.))
                                                                                (st/get-env)
                                                                                nil))
                               via (if-some [n (#'s/spec-name spec)]
                                     [n]
                                     [])
                               ed (merge (assoc (s/explain-data* spec [] via [] data)
                                                ::s/fn (#'s/->sym v)
                                                data-key data
                                                ::s/failure :instrument)
                                         (when caller
                                           {::caller caller}))]
                           (throw (ex-info (str "Call to " v " did not conform to spec.") ed)))
                         conformed))))
        ;; same code as of clojurescript 1.10.764
        pure-variadic? (and (-> (meta v) :top-fn :variadic?)
                            (zero? (-> (meta v) :top-fn :max-fixed-arity)))
        ;; same code as of clojurescript 1.10.764
        apply' (fn [f args]
                 (if (and (nil? args)
                          pure-variadic?)
                   (.cljs$core$IFn$_invoke$arity$variadic f)
                   (apply f args)))
        ret (fn [& args]
              (if @#'st/*instrument-enabled*
                (let [cargs (when args-spec
                              (conform! v :args args-spec args ::s/args))
                      ret (apply' f args)
                      cret (when (:ret fn-spec)
                             (conform! v :ret (:ret fn-spec) ret ::s/ret))]
                  (when-let [spec (:fn fn-spec)]
                    (if (nil? cargs)
                      (throw (no-args-spec v fn-spec))
                      (conform! v :fn spec {:ret (or cret ret) :args cargs} ::s/fn)))
                  ret)
                (apply' f args)))
        conform!* #(conform! v :args args-spec % ::s/args)]
    (when-not pure-variadic?
      (setup-static-dispatches f ret conform!* 20)
      (when-some [variadic (.-cljs$core$IFn$_invoke$arity$variadic f)]
        (set! (.-cljs$core$IFn$_invoke$arity$variadic ret)
              (fn [& args]
                (if @#'st/*instrument-enabled*
                  (do
                    (conform!* (apply list* args))
                    (apply' variadic args))
                  (apply' variadic args))))))
    ret))

(defonce patch-clojure (set! st/spec-checking-fn spec-checking-fn))
