(ns example.simple
  (:require [tilakone.core :as tk :refer [_]]
            [tilakone.schema :as tks]))

; Serializable state description:

(def count-ab
  [{:name        :start
    :transitions [{:on \a, :to :found-a}
                  {:on _}]}
   {:name        :found-a
    :transitions [{:on \a}
                  {:on \b, :to :start, :actions [:inc-val]}
                  {:on _, :to :start}]}])

(tks/validate-states count-ab)
;=> [:name :start, ...

; Non-serializable state:

(def count-ab-process
  {:states  count-ab
   :state   :start
   :action! (fn [fsm signal action]
              (case action
                :inc-val (update fsm :value inc)))
   :value   0})

(tks/validate-fsm count-ab-process)
;=> {:states ...

; Try to send some signals:

(-> count-ab-process
    (tk/apply-signal \a))
;=> {:state :found-a
;    :value 0
;    ...

(-> count-ab-process
    (tk/apply-signal \a)
    (tk/apply-signal \b))
;=> {:state :start
;    :value 1
;    ...

(reduce tk/apply-signal
        count-ab-process
        "abaaabc")
;=> {:state :start
;    :value 2
;    ...

