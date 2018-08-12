# tilakone

Minimalistic finite state machine (FSM) in Clojure.

> **Tilakone (_eng._ State machine)**
> 
> Noun
>  
>   state machine (plural state machines)
>  
>      (computing theory) A formalism for describing computation, consisting of a set of 
>      states and a transition function describing when to move from one state to another.
>
>    _source: [wiktionary.org](https://en.wiktionary.org/wiki/state_machine)_

## Usage

The excellent [cdorrat/reduce-fsm](https://github.com/cdorrat/reduce-fsm) library
has a nice FSM [example](/cdorrat/reduce-fsm#basic-fsm):

```clj
; from https://github.com/cdorrat/reduce-fsm#basic-fsm

(defn inc-val [val & _] (inc val))

(fsm/defsm count-ab
  [[:start
    \a -> :found-a]
   [:found-a
    \a ->  :found-a
    \b -> {:action inc-val} :start
    _ -> :start]])

;; We can use the generated fsm like any function
(map (partial count-ab 0) ["abaaabc" "aaacb" "bbbcab"])
;; returns => (2 0 1)
``` 

This is very nice and workd for many cases. For some _reduce-fsm_ library uses 
macros to define the FSM and actions are functions. This makes the FSM less
suitable if you need to serialize the FSM.

_Tilakone_ is a similar FSM library, but it uses pure data to define the FSM states
(no macros needed) and the action functions can be defined separately.

Here's the same example with _tilakone_:

```clj
(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

(def count-ab-states
  {:start   {:transitions {\a {:state :found-a}
                           _  {:state :start}}}
   :found-a {:transitions {\a {:state :found-a}
                           \b {:state   :start
                               :actions [[:inc-val]]}
                           _  {:state :start}}}})

(def count-ab-fsm
  {:states    count-ab-states
   :action-fn (fn [action value & _]
                (case action
                  :inc-val (inc value)))
   :state     :start
   :value     0})

(def count-ab (partial reduce tk/apply-signal count-ab-fsm))

(->> ["abaaabc" "aaacb" "bbbcab"]
     (map count-ab)
     (map :value))
;=> (2 0 1)
```

Note that the state transfers data `count-ab-states` is pure data.

Also, there is one extra state transfer defined in _tilakone_ example from
state `:state`. In _reduce_fsm_ the default (if no transition is found for
signal) is to stay at the current state, but _tilakone_ treats all undeclared 
state transfers as errors. For this purpose the above example declares an
explicit rule to allow any unmatched signal (`_`) to be handled by 
`{:state :start}`.

The `count-ab-fsm` is the actual FSM. It contains the state declarations,
optional actions function (more of actions later), current state and current
value. Note that the state and value can be any clojure data value.

The `tilakone.core/apply-signal` function accepts an FSM and a signal. It
returns the FSM with possibly updated state and value. 

## Documentation

* TODO

## Comparing reduce-fsm and Tilakone

* reduce-fsm is older project with far more users
* reduce-fsm is faster (at least at the moment)
* reduce-fsm is more feature complete
* tilakone FSMs are pure data™
* tilakone code is quite a bit simpler with no macros and less code (reduce-fsm 592 lines, tilakone 115 lines)

## TODO

* proper documentation
* examples on :enter/:leave actions, state guards, etc
* compare to reduce-fsm
* add fsm visualization
* move schema and ubergraph to modules
* add perf tests

## License

Copyright &copy; 2018 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
