(ns tilakone.core
  (:require [tilakone.util :as u]))


(def _
  "Special symbol to denote that the transition matches any signal"
  ::_)


(defn apply-signal
  "Accepts a FSM (Finite State Machine) and a signal, applies the signal to the FSM
  and returns (possibly) updated FSM."
  [fsm signal]
  (let [from-state (-> fsm ::state)
        transition (-> fsm (u/get-transition signal))
        to-state   (-> transition ::to (or from-state))]
    (-> fsm
        (u/apply-actions signal transition)
        (assoc ::state to-state))))


(defn apply-guards
  "Accepts a FSM and a signal, resolves all transitions that are possible with given
  signal, returns seq of tuples of `[transitions guard-results]`, where `guard-results` are
  results reported by guards. Each result is a map with `:tilakone.core/allow?` (boolean
  indicating guard verdict), `:tilakone.core/guard` (the guard data from fsm), and
  `:tilakone.core/result` (the return value of guard, or an exception)."
  [fsm signal]
  (->> (u/get-transitions fsm signal)
       (map (fn [transition]
              [transition (u/apply-guards fsm signal transition)]))))


(defn transfers-to
  "Accepts a FSM and a signal, returns the name of the state the signal would
  transfer the FSM if applied. Returns `nil` if signal is not allowed."
  [fsm signal]
  (->> (apply-guards fsm signal)
       (u/find-first (fn [[_ guard-results]]
                       (every? ::allow? guard-results)))
       first
       ::to))


(comment

  ;
  ; The FSM looks like this:
  ;

  (def FSM
    {::state   Any ;                                     Current state
     ::states  [{::name        Any ;                     State name (can be string, keyword, symbol, any clojure value)
                 ::desc        Str ;                     Optional state description
                 ::transitions [{::name    Any ;         Transition name
                                 ::desc    Str ;         Transition description
                                 ::to      Any ;         Name of the next state
                                 ::on      Matcher ;     Data for match?, does the signal match this transition?
                                 ::guards  [Guard] ;     Data for guard?, is this transition allowed?
                                 ::actions [Action]}] ;  Actions to be performed on this transition
                 ; Guards and actions used when state is transferred to this stateP
                 ::enter       {::guards  [Guard]
                                ::actions [Action]}
                 ; Guards and actions used when state is transferred from this state:
                 ::leave       {::guards  [Guard]
                                ::actions [Action]}
                 ; Guards and actions used when state transfer is not made:
                 ::stay        {::guards  [Guard]
                                ::actions [Action]}}]
     ::match?  (fn [signal on] ... true/false) ;   Signal matching predicate
     ::guard?  (fn [{:tilakone.core/keys [signal guard] :as fsm}] ... true/false) ;   Guard function
     ::action! (fn [{:tilakone.core/keys [signal action] :as fsm}] ... fsm)}) ;       Action function

  )
