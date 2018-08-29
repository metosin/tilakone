(ns example.full-example
  (:require [tilakone.core :as tk :refer [_]]
            [tilakone.schema :as ts]))

; Action and guard functions are defined in FSM under `:action-fn` and `:guard-fn` respectively.
; Following documentation uses the term "action", but the same rules apply to guards as well.
;
; At the FSM `:states` the action invocations are represented as vectors. These are handled by
; a call `(apply (:action-fn fsm) (first action) (:value fsm) (rest action))`.
;
; For example, if the action in `:states` is defined as `[:foo 1 2 3]`, then the `:action-fn` will
; be called with arguments `:foo`, `(:value fsm)`, `1`, `2`, and `3`.
;
; Actions are expected to return a new value for FSM.


; Transition actions:
;
; Actions can be attached to transitions. These actions are invoked when the transition is performed.
; Actions are performed in the order they are defined.

(def transition-action-fsm
  {:states    {:a {:transitions {\b {:to      :b
                                     :actions [[:log "moving: :a => :b"]]}}}
               :b {:transitions {\a {:to      :a
                                     :actions [[:log "moving: :b => :a"]
                                               [:inc]]}}}}
   :action-fn (fn [action-id value & args]
                (case action-id
                  :log (update value :log conj (first args))
                  :inc (update value :counter inc)))
   :state     :a
   :value     {:log     []
               :counter 0}})

(->> "bababa"
     (reduce tk/apply-signal transition-action-fsm)
     :value)
;=> {:log ["moving: :a => :b"
;          "moving: :b => :a"
;          "moving: :a => :b"
;          "moving: :b => :a"
;          "moving: :a => :b"
;          "moving: :b => :a"],
;    :counter 3}

; State actions:
;
; States can have `:enter`, `:leave`, and `:stay` actions. These are executed when
; signal causes the FSM to enter the state, leave from the state, or stay in the
; state.

(def state-actions-fsm
  {:states    {:a {:transitions {\b {:to :b}
                                 _  {:to :a}}
                   :enter       [[:log "enter :a"]]
                   :leave       [[:log "leave :a"]]
                   :stay        [[:log "stay :a"]]}
               :b {:transitions {_ {:to :a}}
                   :enter       [[:log "enter :b"]
                                 [:inc]]
                   :leave       [[:log "leave :b"]]
                   :stay        [[:log "stay :b"]]}}
   :action-fn (fn [action-id value & args]
                (case action-id
                  :log (update value :log conj (first args))
                  :inc (update value :counter inc)))
   :state     :a
   :value     {:log     []
               :counter 0}})

(->> "babaz"
     (reduce tk/apply-signal state-actions-fsm)
     :value)
;=> {:log ["leave :a"
;          "enter :b"
;          "leave :b"
;          "enter :a"
;          "leave :a"
;          "enter :b"
;          "leave :b"
;          "enter :a"
;          "stay :a"],
;    :counter 2}

; Guards:
;
; So far the signals have been matched against transitions by simply comparing equality. Sometimes
; the transition decision depends on the current value. For this purpose the value matching the
; signal in transitions map can also be a vector containing a guard and the transition. The
; guard is expressed like actions above, that is, a vector containing the guard ID followed
; by optional arguments. The transition is a normal transition as defined above.
;
; Following example declares an FSM that counts the times `"ab"` occurs in a sequence, just
; like in [reduce-fsm example](https://github.com/cdorrat/reduce-fsm#basic-fsm).

(def count-ab-states
  {:start   {:transitions {\a {:to :found-a}
                           _  {:to :start}}}
   :found-a {:transitions {\a {:to :found-a}
                           \b {:to   :start
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

; Now change the FSM so that the maximum number of times the `"ab"` may occur is 3.


(def count-ab-states
  {:start   {:transitions {\a {:to :found-a}
                           _  {:to :start}}}
   :found-a {:transitions {\a {:to :found-a}
                           \b [[:max?] {:actions [[:fail!]]}
                               [_ {:to   :start
                                   :actions [[:inc-val]]}]]
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



;

(def rps-states {:odd  {:transition {:inc [[[:odd?] :odd]
                                           [[:even?] :even]]}}
                 :even {:transition {:inc [[[:odd?] :odd]
                                           [[:even?] :even]]}}})

(defn make-rps-game []
  )


(def fsm
  {:states    {:start   {:transitions {; Simple transition:
                                       "simple" {:to :state-2}
                                       ; Match all signals:
                                       _        {:to :start}}}
               :state-2 {:transitions {; Guarded transition, one signal, multiple transitions:
                                       "guard" [[[:guard-id "guard" "args"] {:to :state-3}]
                                                [[:guard-id "more"] {:to :state-4}]]
                                       ; You can mix guarded and simple transitions:
                                       _       {:to :start}}}
               :state-3 {:transitions {; Transition with transition action:
                                       :to   :state-5
                                       :actions [[:action-1 "action" "args"]]}
                         :enter       [[:enter-action "args"]]
                         :leave       [[:leave-action "args"]]}}
   :action-fn (fn [action value & args] value)
   :guard-fn  (fn [guard value & args] true)
   :state     :start
   :value     0})

;(ts/validate-fsm fsm)
;=> {:states ...

