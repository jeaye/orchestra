(ns orchestra-cljs.spec.test
  (:require-macros [orchestra-cljs.spec.test :refer [instrument unstrument]])
  (:require [cljs.stacktrace :as stack]
            [cljs.spec.alpha :as s]
            [cljs.spec.test.alpha :refer-macros [with-instrument-disabled] :as st]))

(defn no-args-spec
  [v spec]
  (ex-info (str "Args for " v " are not spec'ed.")
           {:var v :spec spec ::s/failure :no-args-spec}))

(defn spec-checking-fn
  [v f raw-fn-spec]
  (let [fn-spec (@#'s/maybe-spec raw-fn-spec)
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
        pure-variadic? (and (-> (meta v) :top-fn :variadic?)
                            (zero? (-> (meta v) :top-fn :max-fixed-arity)))
        apply' (fn [f args]
                 (if (and (nil? args) pure-variadic?)
                   (.cljs$core$IFn$_invoke$arity$variadic f)
                   (apply f args)))
        ret (fn [& args]
              (if @#'st/*instrument-enabled*
                (let [cargs (when (:args fn-spec)
                              (conform! v :args (:args fn-spec) args ::s/args))
                      ret (apply' f args)
                      cret (when (:ret fn-spec)
                             (conform! v :ret (:ret fn-spec) ret ::s/ret))]
                  (when-let [spec (:fn fn-spec)]
                    (if (nil? cargs)
                      (throw (no-args-spec v fn-spec))
                      (conform! v :fn spec {:ret (or cret ret) :args cargs} ::s/fn)))
                  ret)
                (apply' f args)))]
    (when-not pure-variadic?
      (when-some [variadic (.-cljs$core$IFn$_invoke$arity$variadic f)]
        (set! (.-cljs$core$IFn$_invoke$arity$variadic ret)
              (fn [& args]
                (if @#'st/*instrument-enabled*
                  (do
                    (conform! v :args (:args fn-spec) (apply list* args) ::s/args)
                    (apply' variadic args))
                  (apply' variadic args))))))
    ret))

(defonce patch-clojure (set! st/spec-checking-fn spec-checking-fn))
