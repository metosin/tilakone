(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

; State definitions, pure data here:

(def count-ab-states
  [{:name        :start
    :transitions [{:on \a, :to :found-a}
                  {:on _}]}
   {:name        :found-a
    :transitions [{:on \a}
                  {:on \b, :to :start, :actions [:inc-val]}
                  {:on _, :to :start}]}])

; FSM has states, a function to execute actions, and current state and value:

(def count-ab
  {:states  count-ab-states
   :action! (fn [fsm signal action]
              (case action
                :inc-val (update fsm :count inc)))
   :state   :start
   :count   0})

; Lets apply same inputs to our FSM:

(->> ["abaaabc" "aaacb" "bbbcab"]
     (map (partial reduce tk/apply-signal count-ab))
     (map :count))
;=> (2 0 1)
