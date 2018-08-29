(ns example.perf-test
  (:require [tilakone.core :as tk :refer [_]]
            [reduce-fsm :as r]
            [criterium.core :as c]))

;;
;; Tilakone:
;;

(def tk-count-ab-states
  {:start   {:transitions {\a {:to :found-a}
                           _  {:to :start}}}
   :found-a {:transitions {\a {:to :found-a}
                           \b {:to   :start
                               :actions [[:inc-val]]}
                           _  {:to :start}}}})

(def tk-count-ab-fsm
  {:states    tk-count-ab-states
   :action-fn (fn [action value & _]
                (case action
                  :inc-val (inc value)))
   :state     :start
   :value     0})

(def tk-count-ab (partial reduce tk/apply-signal tk-count-ab-fsm))

;;
;; reduce-fsm:
;;

(defn inc-val [val & _] (inc val))

(r/defsm r-count-ab
  [[:start
    \a -> :found-a]
   [:found-a
    \a -> :found-a
    \b -> {:action inc-val} :start
    _ -> :start]])

(defn run-perf-tests []
  (println "\n\nreduce-fsm:")
  (c/quick-bench
    (->> ["abaaabc" "aaacb" "bbbcab"]
         (map (partial r-count-ab 0))
         (dorun)))
  (println "\n\ntilakone:")
  (c/quick-bench
    (->> ["abaaabc" "aaacb" "bbbcab"]
         (map tk-count-ab)
         (dorun))))

(defn -main [& _]
  (run-perf-tests))

(comment
  (run-perf-tests)
  )