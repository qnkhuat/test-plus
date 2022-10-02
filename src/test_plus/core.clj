(ns test-plus.core
  (:require [clojure.test :as t]))

(def ^:dynamic *has-only?* false)

(defonce original-testing (var-get #'clojure.test/testing))
(.setMacro #'original-testing)

(defonce original-deftest (var-get #'clojure.test/deftest))

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

(defn wrap-testing
  [only? f]
  (when (or (not *has-only?*) only?)
    (f)))

(defmacro testing+
  [string & body]
  ;; if a testing from has a testing-only inside it, it should be executed
  (let [has-nested-only?# (form-has-testing-only? body)]
    `(original-testing ~string (wrap-testing ~has-nested-only?# (fn [] ~@body)))))

(defmacro testing-only
  [string & body]
  {:style/indent 1}
  `(original-testing ~string ~@body))

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
  (alter-var-root #'clojure.test/deftest (constantly @#'deftest+))
  nil)

(defn uninstall!
  []
  (ns-unmap 'clojure.test 'testing-only)
  (alter-var-root #'clojure.test/deftest (constantly @#'original-deftest))
  (alter-var-root #'clojure.test/testing (constantly @#'original-testing)))
