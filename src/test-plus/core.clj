(ns test-plus.core
  (:require [clojure.walk :as walk]
            [clojure.test :as t]))

(def ^:dynamic *has-only?* false)

(defn testing*
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

(defmacro deftest+
  [name & body]
  (when t/*load-tests*
    (let [has-only?# (true? (walk/walk
                              first
                              (fn [all]
                                (some #(= (resolve 't/testing-only) (resolve %)) all))
                              body))]
      `(def ~(vary-meta name assoc :test `(fn []
                                            (binding [*has-only?* ~has-only?#] ~@body)))
         (fn [] (t/test-var (var ~name)))))))

(defn install!
  []
  (intern 'clojure.test (with-meta 'testing-only {:macro true}) @#'testing-only)
  (alter-var-root #'clojure.test/testing (constantly @#'testing+))
  (alter-var-root #'clojure.test/deftest (constantly @#'deftest+)))
