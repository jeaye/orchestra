(ns lets-bet.common.spec.detail
  (:require [#?(:clj clojure.spec.alpha
                :cljs cljs.spec.alpha)
             :as s]))

;;;; destructure

(s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(s/def ::binding-form
  (s/or :sym ::local-name
        :seq ::seq-binding-form
        :map ::map-binding-form))

;; sequential destructuring

(s/def ::seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* ::binding-form)
                :rest (s/? (s/cat :amp #{'&} :form ::binding-form))
                :as (s/? (s/cat :as #{:as} :sym ::local-name)))))

;; map destructuring

(s/def ::keys (s/coll-of ident? :kind vector?))
(s/def ::syms (s/coll-of symbol? :kind vector?))
(s/def ::strs (s/coll-of simple-symbol? :kind vector?))
(s/def ::or (s/map-of simple-symbol? any?))
(s/def ::as ::local-name)

(s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(s/def ::map-binding (s/tuple ::binding-form any?))

(s/def ::ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(s/def ::map-bindings
  (s/every (s/or :mb ::map-binding
                 :nsk ::ns-keys
                 :msb (s/tuple #{:as :or :keys :syms :strs} any?)) :into {}))

(s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

(s/def ::spec (s/and some? #(not (string? %)))) ; TODO: Spec for specs
(s/def ::args (s/and vector?
                     (s/cat :args (s/* (s/cat :binding ::binding-form
                                              :spec ::spec))
                            :varargs (s/? (s/cat :amp #{'&}
                                                 :form ::binding-form
                                                 :spec ::spec)))))
(s/def ::arity (s/cat :args ::args
                      :body (s/* any?)))
(s/def ::defn-spec-args (s/cat :name simple-symbol?
                               :ret ::spec
                               :docstring (s/? string?)
                               :meta (s/? map?)
                               :arities (s/alt :single ::arity
                                               :multiple (s/+ (s/spec ::arity)))))

(def ^{:dynamic true} *cljs?* false)

(defn spec-fn [fn-name]
  ; Can't use a map here, since these are macros.
  (if *cljs?*
    (case fn-name
      ::cat 'cljs.spec.alpha/cat
      ::or 'cljs.spec.alpha/or
      ::fdef 'cljs.spec.alpha/fdef)
    (case fn-name
      ::cat 'clojure.spec.alpha/cat
      ::or 'clojure.spec.alpha/or
      ::fdef 'clojure.spec.alpha/fdef)))

(defn render-binding
  "Doing the job of unform, since it turns destructured sequences into lists.
   'cause it's silly."
  [[kind value]]
  (case kind
    :sym value
    :seq (mapv render-binding (:elems value))
    :map value))

(defn explode-arity
  "Strips the specs from the arity's args and gets it ready for consumption.
   Does the job of s/unform, since s/unform doesn't do its job well."
  [arity]
  (let [args (get-in arity [:args :args])
        rendered-args (mapv (comp render-binding :binding) args)
        rendered-varargs (if-some [varargs (get-in arity [:args :varargs])]
                           ['& (render-binding (:form varargs))]
                           [])]
    {::exploded-args (into rendered-args rendered-varargs)
     ::exploded-body (:body arity)}))

(defn render-arity
  "Turns an exploded arity into something which defn can consume."
  [{:keys [::exploded-args ::exploded-body]}]
  (cons exploded-args exploded-body))

(defn extract-arg-specs
  "Returns a sequence of specs, based on the arity's args."
  [arity]
  (let [args (get-in arity [:args :args])
        varargs (get-in arity [:args :varargs])
        arg-specs (mapv :spec args)
        arity-specs (if-some [varargs (get-in arity [:args :varargs])]
                      (conj arg-specs (:spec varargs))
                      arg-specs)]
    arity-specs))

(defn arg->kw
  "Converts are argument to a keyword. Arguments may use destructoring, so they
   may not be a symbol. In that case, just fill in something helpful."
  [idx arg]
  (if (symbol? arg)
    (keyword arg)
    (keyword (str "arg-" idx))))

(defn build-cat
  [arity-arg-names arity-specs]
  (cons (spec-fn ::cat) (interleave arity-arg-names arity-specs)))

(defn name-arity
  [arg-count]
  (keyword (str "arity-" arg-count)))

(defn build-args-spec
  [conformed-arities exploded-arities]
  (let [arg-specs (->> (mapv extract-arg-specs conformed-arities)
                       (sort-by count)) ; Sort for consistency
        arg-names (->> (map (fn [arity]
                              (map-indexed arg->kw (::exploded-args arity)))
                            exploded-arities)
                       (sort-by count)) ; Sort for consistency
        arg-counts (mapv count arg-specs)
        cats (mapv build-cat arg-names arg-specs)
        named-cats (mapcat vector (mapv name-arity arg-counts) cats)]
    (cons (spec-fn ::or) named-cats)))

(defn explode-def
  "Takes in the variadic values of a defn-spec and returns a map of the
   various parts. Handles multiple arities and optional doc strings."
  [& args]
  (let [conformed (->> (s/assert ::defn-spec-args args)
                       (s/conform ::defn-spec-args))
        ; Single arity fns don't require surrounding parens. Conform them to
        ; look like multiple arities before continuing.
        conformed-arities (if (= :single (-> conformed :arities first))
                            [(-> conformed :arities second)]
                            (-> conformed :arities second))
        exploded-arities (mapv explode-arity conformed-arities)
        args-spec (build-args-spec conformed-arities exploded-arities)]
    {::name (:name conformed)
     ::doc (:docstring conformed)
     ::arities (map render-arity exploded-arities)
     ::spec-map (merge (select-keys (:meta conformed) [:fn])
                       (select-keys conformed [:ret])
                       {:args args-spec})}))

(defn defn-spec-helper [& args]
  (let [s-fdef (spec-fn ::fdef)
        exploded (apply explode-def args)
        stripped-meta (dissoc (:meta exploded) :fn)]
    `(do
       (defn ~(::name exploded)
         ~(or (::doc exploded) "")
         ~(or stripped-meta {})
         ~@(::arities exploded))
       (~s-fdef ~(::name exploded)
                :args ~(-> exploded ::spec-map :args)
                :fn ~(-> exploded ::spec-map :fn)
                :ret ~(-> exploded ::spec-map :ret)))))
