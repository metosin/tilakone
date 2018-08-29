(ns example.simple
  (:require [tilakone.core :as tk :refer [_]]
            [tilakone.schema :as tks]))

; Serializable state description:

(def count-ab-states
  {:start   {:transitions {\a {:to :found-a}
                           _  {:to :start}}}
   :found-a {:transitions {\a {:to :found-a}
                           \b {:to   :start
                               :actions [[:inc-val]]}
                           _  {:to :start}}}})

(tks/validate-states count-ab-states)
;=> {:start {:transitions {\a {:to :found-a}
;   ...

; Non-serializable state:

(def count-ab-fsm
  {:states    count-ab-states
   :action-fn (fn [action value & _]
                (case action
                  :inc-val (inc value)))
   :state     :start
   :value     0})

; Try to send some signals:

(-> count-ab-fsm
    (tk/apply-signal \a))
;=> {:state :found-a
;    :value 0
;    ...

(-> count-ab-fsm
    (tk/apply-signal \a)
    (tk/apply-signal \b))
;=> {:state :start
;    :value 1
;    ...

(reduce tk/apply-signal
        count-ab-fsm
        "abaaabc")
;=> {:state :start
;    :value 2
;    ...

