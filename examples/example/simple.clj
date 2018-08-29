(ns example.simple
  (:require [tilakone.core :as tk :refer [_]]
            [tilakone.schema :as tks]))

; Serializable state description:

(def count-ab-states
  [{:name        :start
    :transitions [{:on \a, :to :found-a}
                  {:on _, :to :start}]}
   {:name        :found-a
    :transitions [{:on \a, :to :found-a}
                  {:on \b, :to :start, :actions [:inc-val]}
                  {:on _, :to :start}]}])

(tks/validate-states count-ab-states)
;=> [:name :start, ...

; Non-serializable state:

(def count-ab
  {:states  count-ab-states
   :action! (fn [value signal action]
              (case action
                :inc-val (inc value)))
   :state   :start
   :value   0})

(tks/validate-fsm count-ab)
;=> {:states ...

; Try to send some signals:

(-> count-ab
    (tk/apply-signal \a))
;=> {:state :found-a
;    :value 0
;    ...

(-> count-ab
    (tk/apply-signal \a)
    (tk/apply-signal \b))
;=> {:state :start
;    :value 1
;    ...

(reduce tk/apply-signal
        count-ab
        "abaaabc")
;=> {:state :start
;    :value 2
;    ...

