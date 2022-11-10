(ns test-plus.core
  (:require [clojure.test :as t]))

(def ^:dynamic *deftest-has-only?* false)
(def ^:dynamic *inside-testing-only?* false)

(defonce original-testing (var-get #'clojure.test/testing))
(.setMacro #'original-testing)

(defonce original-deftest (var-get #'clojure.test/deftest))
(.setMacro #'original-deftest)

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
  [testing-has-only? f]
  (when (or
          (not *deftest-has-only?*) ;; if deftest doesn't have testing-only, run it
          *inside-testing-only?*    ;; if this is a sub-testing inside a testing-only, run it
          testing-has-only?)        ;; if deftest has testing-only, only run if this is a testing-only
    (f)))

(defmacro testing+
  [string & body]
  ;; if a testing form has a testing-only inside it, it should be executed
  (let [has-nested-only?# (form-has-testing-only? body)]
    `(original-testing ~string (wrap-testing ~has-nested-only?# (fn [] ~@body)))))

(defmacro testing-only
  [string & body]
  {:style/indent 1}
  `(original-testing ~string (binding [*inside-testing-only?* true]
                               ~@body)))

(defmacro deftest+
  [name & body]
  (let [deftest-has-only?# (form-has-testing-only? body)]
    `(original-deftest ~name (binding [*deftest-has-only?* ~deftest-has-only?#]
                                      ~@body))))

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
  (alter-var-root #'clojure.test/testing (constantly @#'original-testing))
  nil)
