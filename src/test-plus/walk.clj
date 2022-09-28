(ns test-plus.walk
  (:require [clojure.walk :as walk]
            [clojure.test :as t]))

(def ^:dynamic *has-only?* false)

(defn testing*
  [only? f]
  (when (or (not *has-only?*) only?)
    (f)))

(defmacro testing
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [string & body]
  `(binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
    (testing* false (fn [] ~@body))))

(defmacro testing-only
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [string & body]
  `(binding [t/*testing-contexts* (conj t/*testing-contexts* ~string)]
     (testing* true (fn [] ~@body))))

(defmacro deftest
  [name & body]
  (when t/*load-tests*
    (let [has-only?# (true? (walk/walk
                              first
                              (fn [all]
                                (some #(= 'testing-only %) all))
                              body))]
      `(def ~(vary-meta name assoc :test `(fn []
                                            (binding [*has-only?* ~has-only?#] ~@body)))
         (fn [] (t/test-var (var ~name)))))))


(deftest my-test
  (testing "ngoc"
    (println "normal test")
    (t/is (= 1 1)))

  (testing-only "ngoc"
    (println "only test")
    (t/is (= 1 1))))
