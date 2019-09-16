(ns tilakone.core
  (:require [tilakone.util :as u]))


(def _
  "Special symbol to denote that the transition matches any signal"
  :_)


(defn apply-signal
  "Accepts a FSM (Finite State Machine) and a signal, applies the signal to the FSM
  and returns (possibly) updated FSM."
  [fsm signal]
  (let [from-state (-> fsm :state)
        transition (-> fsm (u/get-transition signal))
        to-state   (-> transition :to (or from-state))]
    (-> fsm
        (u/apply-actions signal transition)
        (assoc :state to-state))))


(defn apply-guards
  "Accepts a FSM and a signal, resolves all transitions that are possible with given
  signal, returns seq of tuples of `[transitions guard-errors]`, where `guard-errors` are
  errors reported by guards. If none of the guards report any errors for transition then
  `guard-errors` is `nil`."
  [fsm signal]
  (->> (u/get-transitions fsm signal)
       (map (fn [transition]
              [transition (u/apply-guards fsm signal transition)]))))


(defn transfers-to
  "Accepts a FSM and a signal, returns the name of the state the signal would
  transfer the FSM if applied. Returns `nil` if signal is not allowed."
  [fsm signal]
  (->> (apply-guards fsm signal)
       (u/find-first (complement second))
       first
       :to))


(comment

  ;
  ; The FSM looks like this:
  ;

  (def FSM
    {:state   Any ;                                     Current state
     :states  [{:name        Any ;                     State name (can be string, keyword, symbol, any clojure value)
                :desc        Str ;                     Optional state description
                :transitions [{:name    Any ;         Transition name
                               :desc    Str ;         Transition description
                               :to      Any ;         Name of the next state
                               :on      Matcher ;     Data for match?, does the signal match this transition?
                               :guards  [Guard] ;     Data for guard?, is this transition allowed?
                               :actions [Action]}] ;  Actions to be performed on this transition
                ; Guards and actions used when state is transferred to this stateP
                :enter       {:guards  [Guard]
                              :actions [Action]}
                ; Guards and actions used when state is transferred from this state:
                :leave       {:guards  [Guard]
                              :actions [Action]}
                ; Guards and actions used when state transfer is not made:
                :stay        {:guards  [Guard]
                              :actions [Action]}}]
     :match?  (fn [signal on] ... true/false) ;   Signal matching predicate
     :guard?  (fn [fsm guard] ... true/false) ;   Guard matching predicate
     :action! (fn [fsm action] ... value)}) ;     Action function

  )
