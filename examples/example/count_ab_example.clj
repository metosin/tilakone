(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

; State definitions, pure data here:

(def count-ab-states
  [{::tk/name        :start
    ::tk/transitions [{::tk/on \a, ::tk/to :found-a}
                      {::tk/on _}]}
   {::tk/name        :found-a
    ::tk/transitions [{::tk/on \a}
                      {::tk/on \b, ::tk/to :start, ::tk/actions [:inc-val]}
                      {::tk/on _, ::tk/to :start}]}])

; FSM has states, a function to execute actions, and current state and value:

(def count-ab
  {::tk/states  count-ab-states
   ::tk/action! (fn [{::tk/keys [action] :as fsm}]
                  (case action
                    :inc-val (update fsm :count inc)))
   ::tk/state   :start
   :count       0})

; Lets apply same inputs to our FSM:

(->> ["abaaabc" "aaacb" "bbbcab"]
     (map (partial reduce tk/apply-signal count-ab))
     (map :count))
;=> (2 0 1)
