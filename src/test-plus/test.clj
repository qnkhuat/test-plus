(ns test-plus.test)



(def ^:dynamic a-var nil)

(defmacro testing.only
  [string]
  (println "avar: "a-var)
  `(println ~string))


(binding [a-var "string"]
  (testing.only "abc"))

