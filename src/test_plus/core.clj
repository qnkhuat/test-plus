(ns test-plus.core
  (:require [clojure.test :as t]))

(def ^:dynamic *has-only?* false)

(defn- testing*
  [only? f]
  (when (or (not *has-only?*) only?)
    (f)))

(defmacro testing+
  [string & body]
  `(binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
     (testing* false (fn [] ~@body))))

(defmacro testing-only
  "Run only this"
  [string & body]
  `(binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
     (testing* true (fn [] ~@body))))

(defn- testing-only? [x]
  (and (symbol? x)
       (= (resolve 'clojure.test/testing-only) (ns-resolve *ns* x))))

(defn- form-has-testing-only?
  "Returns true if the form contains at least one `testing-only`."
  [form]
  (boolean (some true?
                 (for [sub-form form]
                   (if (seq? sub-form)
                     (form-has-testing-only? sub-form)
                     (testing-only? sub-form))))))

(defmacro deftest+
  [name & body]
  (when t/*load-tests*
    (let [has-only?# (form-has-testing-only? body)]
      `(def ~(vary-meta name assoc :test `(fn []
                                            (binding [*has-only?* ~has-only?#] ~@body)))
         (fn [] (t/test-var (var ~name)))))))

(defn install!
  []
  (intern 'clojure.test (with-meta 'testing-only {:macro true}) @#'testing-only)
  (alter-var-root #'clojure.test/testing (constantly @#'testing+))
  (alter-var-root #'clojure.test/deftest (constantly @#'deftest+)))

(comment
  (install!)

  (t/deftest what
    (t/testing-only "yes"
                    (println "YES"))
    (t/testing "no"
      (println "NO")))


  (t/deftest nested
    (t/testing "SUP"
      (t/testing-only "yes"
                      (println "YES"))
      (t/testing "no"
        (println "NO"))))

  (t/deftest no-only
    (t/testing "SUP"
      (t/testing "no"
        (println "no"))
      (t/testing "no"
        (println "NO")))))
