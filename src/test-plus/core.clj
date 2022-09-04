(ns test-plus.core
  (:require [clojure.test :as t]))



;; NOTE:
;; the t/testing will append the test string to t/*testing-contexts*




(t/deftest arithmetic-test
 #p t/*testing-contexts*
 (t/testing "first-level"
   #p t/*testing-contexts*
   (t/is (= 1 (inc 0)))

   (t/testing "nested"
     #p t/*testing-contexts*
     (t/is (= 1 (inc 0))))))

(vals (ns-interns *ns*))
