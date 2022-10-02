[![Clojars Project](https://clojars.org/com.github.qnkhuat/test-plus/latest-version.svg)](https://clojars.org/com.github.qnkhuat/test-plus)

Do you often have a `deftest` with long and deep `testing` inside, and you wish there is a way you can run one specific `testing` inside it?

If you do, then you should use `test-plus`, it's a drop-in tooling that introduces a new macro `testing-only` that conveniently making Clojure.test to run one single `testing`.

### How to use it?

```clojure
(ns test-namespace
  (:require [:clojure.test :as t]
            [test-plus.core :as test-plus]))

(test-plus/install!) ;; this only have to be done once, so you might want to include this in your testing entry

(t/deftest a-big-test
  (t/testing "first layer"
    (t/testing "second layer"
      (t/is (= 2 (inc 1))))

    ;; you want only this test to be executen
    (t/testing-only "run this only"
      (t/is (= 2 (+ 1 1))))))

;; run the test
(t/test-run-var 'a-big-test)
```
