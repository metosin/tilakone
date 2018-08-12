(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

(def count-ab-states
  {:start   {:transitions {\a {:state :found-a}
                           _  {:state :start}}}
   :found-a {:transitions {\a {:state :found-a}
                           \b {:state   :start
                               :actions [[:inc-val]]}
                           _  {:state :start}}}})

(def count-ab-fsm
  {:states    count-ab-states
   :action-fn (fn [action value & _]
                (case action
                  :inc-val (inc value)))
   :state     :start
   :value     0})

(def count-ab (partial reduce tk/apply-signal count-ab-fsm))

(->> ["abaaabc" "aaacb" "bbbcab"]
     (map count-ab)
     (map :value))
;=> (2 0 1)
