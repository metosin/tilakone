(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            [eftest.runner :as eftest]
            [eftest.report.pretty :as pretty]))

(def reset repl/refresh)
(def start (constantly :ok))
(def stop (constantly :ok))

(def test-dirs (->> ["core" "schema" "graph"]
                    (map (fn [module]
                           (str "modules/" module "/test")))))

(defn run-unit-tests []
  (eftest/run-tests
    (->> (mapcat eftest.runner/find-tests test-dirs)
         (remove (comp :integration meta))
         (remove (comp :slow meta)))
    {:multithread? true
     :report       pretty/report}))

(defn run-all-tests []
  (eftest/run-tests
    (mapcat eftest.runner/find-tests test-dirs)
    {:multithread? false
     :report       pretty/report}))
