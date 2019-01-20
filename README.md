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

[![Clojars Project](https://img.shields.io/clojars/v/metosin/tilakone.svg)](https://clojars.org/metosin/tilakone)

## Usage

All bundled:

```clj
[metosin/tilakone "0.0.4"]
```

Optionally, the modules can be required separately:

```clj
[metosin/tilakone.core "0.0.4"]
[metosin/tilakone.schema "0.0.4"]
```

## Intro

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

This is very nice and works for many cases. The _reduce-fsm_ library uses 
macros to define the FSM and actions are functions. This makes the _reduce-fsm_ less
suitable if you need to serialize your FSMs.

_Tilakone_ is a similar FSM library, but it uses pure data to define the FSM states
(no macros needed) and the action functions can be defined separately.

Here's the same example with _tilakone_:

```clj
(ns example.count-ab-example
  (:require [tilakone.core :as tk :refer [_]]))

; State definitions, pure data here:

(def count-ab-states
  [{::tk/name        :start
    ::tk/transitions [{::tk/on \a, ::tk/to :found-a}
                      {::tk/on _}]}
   {::tk/name        :found-a
    ::tk/transitions [{::tk/on \a}
                      {::tk/on \b, ::tk/to :start, ::tk/actions [:inc-val]}
                      {::tk/on _, ::tk/to :start}]}])

; FSM has states, a function to execute actions, and current state and value:

(def count-ab
  {::tk/states  count-ab-states
   ::tk/action! (fn [fsm signal action]
                  (case action
                    :inc-val (update fsm :count inc)))
   ::tk/state   :start
   :count       0})

; Lets apply same inputs to our FSM:

(->> ["abaaabc" "aaacb" "bbbcab"]
     (map (partial reduce tk/apply-signal count-ab))
     (map :count))
;=> (2 0 1)
```

Note that the state definitions in `count-ab-states` are pure data.

Also, there is one extra state transfer defined in _tilakone_ example from
state `:start`. In _reduce_fsm_ the default behaviour for signal (that is, if no 
transition is found for signal) is to stay at the current state, but _tilakone_ 
treats all undeclared state transfers as errors. For this purpose the above example 
declares an explicit rule to allow any unmatched signal (`_`) in state `:start` to be 
handled as transition to state `:start`.

The `count-ab` is the actual FSM. It contains the state declarations,
optional actions function (more of actions later), current state and current
value. Note that the state and value can be any clojure value.

The `tilakone.core/apply-signal` function accepts an FSM and a signal. It
returns the FSM with possibly updated state and value. 

## Documentation

* TODO

## Comparing reduce-fsm and Tilakone

* reduce-fsm is older project with far more users
* reduce-fsm is faster (at least at the moment)
* reduce-fsm is more feature complete
* tilakone FSMs are pure data™
* tilakone code is quite a bit simpler with no macros and less code (reduce-fsm 592 lines, tilakone 145 lines)

## TODO

* proper documentation
* examples on :enter/:leave actions, state guards, etc
* add tilakone visualization
* add perf tests

## License

Copyright &copy; 2019 [Metosin Oy](https://www.metosin.fi/)

Distributed under the Eclipse Public License, the same as Clojure.
