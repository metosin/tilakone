(ns example.fn-actions-and-guards
  (:require [tilakone.core :as tk :refer [_]]))

; You do not have use pure data states, you can use functions as guards and actions easily:

(def count-ab
  [{:name        :start
    :transitions [{:on \a, :to :found-a}
                  {:on _}]}
   {:name        :found-a
    :transitions [{:on \a}
                  {:on \b, :to :start, :actions [#(-> % :process :value inc)]}
                  {:on _, :to :start}]}])


(def count-ab-process
  {:states  count-ab
   :action! (fn [ctx] ((-> ctx :action) ctx))
   :state   :start
   :value   0})


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

