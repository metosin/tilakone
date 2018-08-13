(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

(def count-ab-states
  {:start   {:transitions {\a {:state :found-a}
                           _  {:state :start}}}
   :found-a {:transitions {\a {:state :found-a}
                           \b {:state   :start
                               :actions [[:inc-val]]}
                           _  {:state :start}}}})

(def count-ab
  {:states    count-ab-states
   :action-fn (fn [action value & _]
                (case action
                  :inc-val (inc value)))
   :state     :start
   :value     0})

(->> ["abaaabc" "aaacb" "bbbcab"]
     (map (partial reduce tk/apply-signal count-ab))
     (map :value))
;=> (2 0 1)
