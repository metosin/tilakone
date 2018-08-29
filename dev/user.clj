(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [eftest.runner :as eftest]))

(def reset repl/refresh)
(def start (constantly :ok))
(def stop (constantly :ok))

(defn run-unit-tests []
  (eftest/run-tests
    (->> (eftest.runner/find-tests "test")
         (remove (comp :integration meta))
         (remove (comp :slow meta)))
    {:multithread? true}))

(defn run-all-tests []
  (eftest/run-tests
    (eftest.runner/find-tests "test")
    {:multithread? false}))
