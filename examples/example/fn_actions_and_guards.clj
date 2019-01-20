(ns example.fn-actions-and-guards
  (:require [tilakone.core :as tk :refer [_]]))

; You do not have use pure data states, you can use functions as guards and actions easily:

(def count-ab
  [{::tk/name        :start
    ::tk/transitions [{::tk/on \a, ::tk/to :found-a}
                      {::tk/on _}]}
   {::tk/name        :found-a
    ::tk/transitions [{::tk/on \a}
                      {::tk/on \b, ::tk/to :start, ::tk/actions [#(update % :count inc)]}
                      {::tk/on _, ::tk/to :start}]}])


(def count-ab-process
  {::tk/states  count-ab
   ::tk/action! (fn [{::tk/keys [action] :as fsm}] (action fsm))
   ::tk/state   :start
   :count       0})


(-> count-ab-process
    (tk/apply-signal \a))
;=> {::tk/state ::tk/found-a
;    :count 0
;    ...


(-> count-ab-process
    (tk/apply-signal \a)
    (tk/apply-signal \b))
;=> {::tk/state :start
;    :count 1
;    ...


(reduce tk/apply-signal
        count-ab-process
        "abaaabc")
;=> {::tk/state :start
;    :count 2
;    ...

