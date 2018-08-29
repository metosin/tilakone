(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

(def count-ab-states
  {:start   {:transitions {\a {:to :found-a}
                           _  {:to :start}}}
   :found-a {:transitions {\a {:to :found-a}
                           \b {:to      :start
                               :actions [[:inc-val]]}
                           _  {:to :start}}}})

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
