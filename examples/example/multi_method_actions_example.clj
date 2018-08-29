(ns example.multi-method-actions-example
  (:require [tilakone.core :as tk :refer [_]]))

(def count-ab-states
  {:start   {:transitions {\a {:to   :found-a
                               :actions [[:state-change :start :found-a]]}
                           _  {:to   :start
                               :actions [[:state-change :start :start]]}}}
   :found-a {:transitions {\a {:to   :found-a
                               :actions [[:state-change :found-a :found-a]]}
                           \b {:to   :start
                               :actions [[:inc-val]
                                         [:state-change :found-a :start]]}
                           _  {:to   :start
                               :actions [[:inc-val]
                                         [:state-change :found-a :start]]}}}})

(defmulti count-action (fn [action & _] action))

(defmethod count-action :inc-val [_ value & _]
  (println "inc-val:" value "=>" (inc value))
  (inc value))

(defmethod count-action :state-change [_ value from-state to-state & _]
  (println "state-change: " from-state "=>" to-state)
  value)

(def count-ab
  {:states    count-ab-states
   :action-fn count-action
   :state     :start
   :value     0})

(reduce tk/apply-signal
        count-ab
        "abaaabc")
; Prints:
;  state-change:  :start => :found-a
;  inc-val: 0 => 1
;  state-change:  :found-a => :start
;  state-change:  :start => :found-a
;  inc-val: 1 => 2
;  state-change:  :found-a => :start
;=> {:to :start,
;    :value 2,
;    :states {...

