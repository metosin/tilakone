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
  {:states [{:name        "green"
             :transitions [{:on "TIMER"
                            :to "yellow"}]}
            {:name        "yellow"
             :transitions [{:on "TIMER"
                            :to "red"}]}
            {:name        "red"
             :transitions [{:on "TIMER"
                            :to "green"}]}]
   :state  "green"})

(->> (repeat 5 "TIMER")
     (reduce tk/apply-signal light-machine)
     :state)
;=> "red"
