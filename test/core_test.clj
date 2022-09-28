(ns test-core
  (:require [clojure.test :as t]))


(t/deftest my-test+
  (t/testing "ngoc"
    (println "normal test")
    (t/is (= 1 1)))

  (t/testing-only "ngoc"
                  (println "only test")
                  (t/is (= 1 1))))


#_(tp/deftest+ my-test
    (tp/testing+ "ngoc"
              (println "normal test")
              (t/is (= 1 1)))

    (testing-only "ngoc"
                  (println "only test")
                  (t/is (= 1 1))))
