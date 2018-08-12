(ns tilakone.core
  (:require [tilakone.util :as u]))

(def _
  "Special symbol to denote that the transition matches any signal"
  ::_)

(defn apply-signal [fsm signal]
  (let [current-state-key (-> fsm :state)
        current-state (-> fsm :states (get current-state-key))
        transition (u/get-transition fsm current-state signal)
        next-state-key (-> transition :state)
        next-state (-> fsm :states (get next-state-key))]
    (if (= next-state-key current-state-key)
      (-> fsm
          (u/apply-actions (-> current-state :stay)))
      (-> fsm
          (u/apply-actions (-> current-state :leave))
          (assoc :state next-state-key)
          (u/apply-actions (-> transition :actions))
          (u/apply-actions (-> next-state :enter))))))


; mental notes:
;
;(def FSM {:states    {'State {:transitions {'Signal 'Transition}
;                              :enter       ['Action]
;                              :leave       ['Action]
;                              :stay        ['Action]}}
;          :action-fn 'IFn
;          :guard-fn  'IFn
;          :state     'Any
;          :value     'Any})
;
;(def Transition (or SimpleTransition GuardedTransition))
;
;(def SimpleTransition
;  {:state   'State
;   :actions ['Action]
;   'Keyword 'Any})
;
;(def GuardedTransition
;  {'Guard 'SimpleTransition})
;