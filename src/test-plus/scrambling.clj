(ns test-plus.scamble
  (:require [clojure.test :as t]))

(intern 'clojure.test (with-meta 'testing.skip {:macro true}) @#'testing.skip)

(def test-stats (atom {}))

(def ^:dynamic *test-parent*)

(defn my-testing
  [only? f]
  (when (or (= (get-in @test-stats [(butlast t/*testing-contexts*) :only] 0) 0)
            only?)
    (f)))

(defn update-stat [stat context]
  (swap! stat (fn [stat] (update-in stat [context :only] (fn [x] (if x (inc x) 1))))))

(defmacro testing
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [string & body]
  `(binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
     (my-testing false (fn [] ~@body))))

(defmacro testing-only
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [string & body]
  `(do
     (update-stat test-stats (conj t/*testing-contexts* ~string))
     (binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
       (my-testing true (fn [] ~@body)))))

(defmacro deftest
  "Defines a test function with no arguments.  Test functions may call
  other tests, so tests may be composed.  If you compose tests, you
  should also define a function named test-ns-hook; run-tests will
  call test-ns-hook instead of testing all vars.

  Note: Actually, the test body goes in the :test metadata on the var,
  and the real function (the value of the var) calls test-var on
  itself.

  When *load-tests* is false, deftest is ignored."
  {:added "1.1"}
  [name & body]
  (when t/*load-tests*
  (binding [*test-parent* name]
   `(def ~(vary-meta name assoc :test `(binding [*test-parent* "SUP"]
                                         (fn [] ~@body)))
      (fn []
          (test-var (var ~name)))))))

(deftest simple-test
  (testing "n"
    (testing "ngockq"
      (println "Testing")
      (t/is (= 2 (+ 1 "1"))))

    (testing-only "ngockq2"
                  (println "Testing")
                  (t/is (= 2 (+ 1 1))))))

@test-stats

(test-vars [#'simple-test])

(defn test-var
  "If v has a function in its :test metadata, calls that function,
  with *testing-vars* bound to (conj *testing-vars* v)."
  {:dynamic true, :added "1.1"}
  [v]
  (when-let [t (:test (meta v))]
    (binding [t/*testing-vars* (conj t/*testing-vars* v)
              *test-stats*     (atom {:only 0 :skip 0})]
      (t/do-report {:type :begin-test-var, :var v})
      (t/inc-report-counter :test)
      (try (t)
           (catch Throwable e
             (t/do-report {:type :error, :message "Uncaught exception, not in assertion."
                         :expected nil, :actual e})))
      (t/do-report {:type :end-test-var, :var v}))))

(defn test-vars
  "Groups vars by their namespace and runs test-var on them with
  appropriate fixtures applied."
  {:added "1.6"}
  [vars]
  (doseq [[ns vars] (group-by (comp :ns meta) vars)]
    (let [once-fixture-fn (t/join-fixtures (::once-fixtures (meta ns)))
          each-fixture-fn (t/join-fixtures (::each-fixtures (meta ns)))]
      (once-fixture-fn
       (fn []
         (doseq [v vars]
           (when (:test (meta v))
             (each-fixture-fn (fn [] (test-var v))))))))))

((:test (meta #'simple-test)))

(test-vars [#'simple-test])

(testing.skip "ngockq"
              (println "AHHH"))

(meta 't/testing.skip2)

(t/testing.skip
  "ngock" (println "abc"))

(intern 'clojure.test 'random2 (defmacro random [x] `(println ~x)))

#'t/random

(t/random2 "sup")

(defmacro tt
  [string & body]
  (println (meta string))
  )

(macroexpand-1 '(tt ^:ngoc "ngoc"))

(meta #'ngoc)

(def ^:sup quang "abc")

(meta (with-meta [1 2 3] {:sup "sup"}))


;; NOTE:
;; What we need to do seems to be resolve around the test running functions.
;; the deftest macro returns a function that we can call
;; and the `test-var` or run test function just run them as a function


;; this is a bit harder than I thought:
;; - deftest is actually just a macro expands its body as a function
;; - each `testing` inside is basically just expanding the body, so we have no way to have a post-evaluation to "ignore this body because there is one "only" testing.
;; - if we want to do this we might have to rewrite how we execute tests, and it's could be not practical

;; something to loko into : https://github.com/circleci/circleci.test/blob/master/src/circleci/test.clj
(t/deftest arithmetic-test
 #p t/*testing-contexts*
 (t/underline-testing true "first-level"
                      #p t/*testing-contexts*
                      (t/is (= 1 (inc 0)))

                      (t/testing "nested"
                        #p t/*testing-contexts*
                        (t/is (= 1 (inc 0))))))

(macroexpand-1 '(t/underline-testing true "first-level"
                                     (println "sup")))

(macroexpand-1 '(t/testing "first-level"
                  (println "sup")))

(t/deftest sup?
  (t/underline-testing true "first-level"
                       (println "sup")))


(macroexpand '(t/deftest testing
                  (macroexpand-1 '(t/testing "no has only"
                                    (println "SUPPPPPPP")))))




(def ^:dynamic *a* nil)

(def f (fn []
         (binding [*a* (atom false)]
           (println *a*))))

(f)





(macroexpand '(t/deftest testing2
                (t/testing.only "has only"
                                (println t/*has-only*))))

(macroexpand-1 '(t/deftest testing2
                  (t/testing.only "has only"
                                  (println t/*has-only*))))

(macroexpand-1 '(t/testing "no has only"
                       (println t/*has-only*)))


(macroexpand-1 '(t/deftest test
                 (println t/*has-only*)))

(t/deftest test
  (t/testing "ngoc"
    (println t/*has-only*))

  (t/testing.only "ngoc"
    (println t/*has-only*)))




#_(t/run-test-var arithmetic-test)

(t/run-test arithmetic-test)

(vals (ns-interns *ns*))
