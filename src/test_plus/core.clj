(ns test-plus.core
  (:require [clojure.test :as t]))

(def ^:dynamic *deftest-has-only?* false)
(def ^:dynamic *inside-testing-only?* false)

(defonce original-deftest (var-get #'clojure.test/deftest))
(.setMacro #'original-deftest)

(defonce original-testing (var-get #'clojure.test/testing))
(.setMacro #'original-testing)

(defn- testing-only? [x]
  (and (symbol? x)
       (= (resolve 'clojure.test/testing-only) (ns-resolve *ns* x))))

(defn- form-has-testing-only?
  "Walk the form and return `true` if it contains at least one `testing-only`."
  [form]
  (boolean (some true?
                 (for [sub-form form]
                   (if (seq? sub-form)
                     (form-has-testing-only? sub-form)
                     (testing-only? sub-form))))))

(defn wrap-testing
  "A `testing` will be executed if one of these conditions are met:
  - There are no `testing-only` in the same `deftest`
  - The current `testing` are nested inside a `testing-only`
  - The current `testing` has one `testing-only` nested inside it"
  [has-nested-only? f]
  (when (or
          (not *deftest-has-only?*) ;; If deftest doesn't have testing-only inside, execute it
          *inside-testing-only?*    ;; If this is a sub-testing inside a testing-only, run it
          has-nested-only?)         ;; If there is at least one `testing-only` inside, run it
    (f)))

(defmacro testing+
  "Like `clojure.test/testing`, but this `testing+` could not be executed if all of these conditions are met:
  - There is a `testing-only` inside the parent `deftest`
  - This `testing+` are not nested under a `testing-only`
  "
  [string & body]
  {:style/indent 1}
  (let [has-nested-only?# (form-has-testing-only? body)]
    `(original-testing ~string (wrap-testing ~has-nested-only?# (fn [] ~@body)))))

(defmacro testing-only
  "Mark a `testing` to be run only. The test runner will skip others `testing` that are not nested under a `testing-only`.

  (t/deftest simple-test
    (with-random-user user
      (t/testing \"user has firstname?\"                ;; <- skip
        (t/is (some? (:first-name user))))

      (t/testing-only \"user has lastname?\"            ;; <- executed
        (t/is (some? (:last-name user)))

        (t/testing \"last-name has at least 4 chars\"   ;; <- executed
          (t/is (>= 4 (count (:last-name user))))))

      (t/testing \"user has password?\"                 ;; <- skip
        (t/is (some? (:password user))))))
  "
  [string & body]
  {:style/indent 1}
  `(original-testing ~string (binding [*inside-testing-only?* true]
                               ~@body)))

(defmacro deftest+
  "Like `clojure.core/deftest`, with one added ability to use `testing-only`.
  This will eventually call the `clojure.core/deftest` to make sure it works seaminglessly
  with `cloure.core/test` runner.

  The only thing it does is it walks the `body` to check if it contains at least one `testing-only`.
  The result is bound to *deftest-has-only?*, it's later used in `testing` and `testing-only` to decide
  whether or not to executes its body."
  [name & body]
  (let [deftest-has-only?# (form-has-testing-only? body)]
    `(original-deftest ~name (binding [*deftest-has-only?* ~deftest-has-only?#]
                               ~@body))))

(defn- copy-meta
  [to with]
  (alter-meta! to merge (select-keys (meta with) [:ns :name :file :column :line])))

(defn install!
  "Install `test-plus`.
  It'll replaces:
  - `clojure.core/deftest` with `test-plus.core/deftest+`
  - `clojure.core/testing` with `test-plus.core/testing+`
  And introduce a new `clojure.core/testing-only` macro.
  "
  []
  (intern 'clojure.test (with-meta 'testing-only {:macro true}) @#'testing-only)
  ;(copy-meta #'clojure.test/testing-only #'testing-only)

  (alter-var-root #'clojure.test/testing (constantly @#'testing+))
  (copy-meta #'clojure.test/testing #'testing+)

  (alter-var-root #'clojure.test/deftest (constantly @#'deftest+))
  (copy-meta #'clojure.test/deftest #'deftest+)
  nil)

(defn uninstall!
  "Uninstall `test-plus`"
  []
  (ns-unmap 'clojure.test 'testing-only)
  (alter-var-root #'clojure.test/deftest (constantly @#'original-deftest))
  (copy-meta #'clojure.test/deftest #'original-deftest)

  (alter-var-root #'clojure.test/testing (constantly @#'original-testing))
  (copy-meta #'clojure.test/testing #'original-testing)
  nil)
