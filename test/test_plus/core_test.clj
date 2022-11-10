(ns test-plus.core-test
  (:require [clojure.test :as t]
            [test-plus.core :as test-plus]))

(test-plus/install!)

(def ^:dynamic *ran-tests* nil)

(defn- mark-test
  [tests id]
  (swap! tests conj id))

(def ^:private executed (atom false))

(t/deftest a-tests-wihtout-testing-only-tests
  (reset! executed true)
  (binding [*ran-tests* (atom #{})]
    (t/testing "A test that doesn't have `testing-only` shoudl works"
      (t/testing "nested"
        (mark-test *ran-tests* :a))
      (t/testing "nested"
        (mark-test *ran-tests* :b)))

    ;; this is the real tests
    (t/is (= @*ran-tests* #{:a :b}))))

;; HACK: these a-, x- naming are intentional because we want this test to be run
;; after the above teset
(test-plus/original-deftest x-previous-test-should-be-executed
  (t/is (= true @executed)))

(t/deftest installed-tests
  (binding [*ran-tests* (atom #{})]
    (t/testing "Including `testing-only` should execute only the code path that led to it"
      (t/testing "a"
        (mark-test *ran-tests* :a)

        (t/testing "a-a"
          (mark-test *ran-tests* :a-a)

          (t/testing "a-a-a shouldn't be executed"
            (mark-test *ran-tests* :a-a-a))

          (t/testing-only "a-a-b"
            (mark-test *ran-tests* :a-a-b)

            (t/testing "a-a-b-a sub-testing of testing-only should be executed"
              (mark-test *ran-tests* :a-a-b-a)))

         (t/testing "a-a-c shouldn't be executed"
           (mark-test *ran-tests* :a-a-c)))

        (t/testing "a-b shouldn't be executed"
          (mark-test *ran-tests* :a-b))))

    ;; this is the real tests
    (t/is (= @*ran-tests* #{:a :a-a :a-a-b :a-a-b-a}))))
