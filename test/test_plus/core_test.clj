(ns test-plus.core-test
  (:require [clojure.test :as t]
            [test-plus.core :as test-plus]))

(test-plus/install!)

(def ^:dynamic *tests* nil)

(defn- mark-test
  [tests id]
  (swap! tests conj id))

(t/deftest tests-wihtout-testing-only-tests
  (binding [*tests* (atom #{})]
    (t/testing "A test that doesn't have `testing-only` shoudl works"
      (t/testing "nested"
        (mark-test *tests* :a))
      (t/testing "nested"
        (mark-test *tests* :b)))

    ;; this is the real tests
    (t/is (= @*tests* #{:a :b}))))

(t/deftest installed-tests
  (binding [*tests* (atom #{})]
    (t/testing "Including `testing-only` should execute only the code path that led to it"
      (t/testing "a"
        (mark-test *tests* :a)

        (t/testing "a-a"
          (mark-test *tests* :a-a)

          (t/testing "a-a-a shouldn't be executed"
            (mark-test *tests* :a-a-a))

          (t/testing-only "a-a-b"
            (mark-test *tests* :a-a-b))

          (t/testing "a-a-c shouldn't be executed"
            (mark-test *tests* :a-a-c)))

        (t/testing "a-b shouldn't be executed"
          (mark-test *tests* :a-b))))

    ;; this is the real tests
    (t/is (= @*tests* #{:a :a-a :a-a-b}))))
