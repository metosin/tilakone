(ns example.light-machine
  (:require [tilakone.core :as tk]))

;; https://github.com/davidkpiano/xstate#finite-state-machines

; import { Machine } from 'xstate';
;
; const lightMachine = Machine({
;   key: 'light',
;   initial: 'green',
;   states: {
;     green: {
;       on: {
;         TIMER: 'yellow',
;       }
;     },
;     yellow: {
;       on: {
;         TIMER: 'red',
;       }
;     },
;     red: {
;       on: {
;         TIMER: 'green',
;       }
;     }
;   }
; });
;

(def light-machine
  {::tk/states [{::tk/name        "green"
                 ::tk/transitions [{::tk/on "TIMER"
                                    ::tk/to "yellow"}]}
                {::tk/name        "yellow"
                 ::tk/transitions [{::tk/on "TIMER"
                                    ::tk/to "red"}]}
                {::tk/name        "red"
                 ::tk/transitions [{::tk/on "TIMER"
                                    ::tk/to "green"}]}]
   ::tk/state  "green"})

(->> (repeat 5 "TIMER")
     (reduce tk/apply-signal light-machine)
     ::tk/state)
;=> "red"
