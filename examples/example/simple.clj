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
   :action! (fn [{:keys [process action]}]
              (case action
                :inc-val (-> process :value inc)))
   :state   :start
   :value   0})

(tks/validate-process count-ab-process)
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

