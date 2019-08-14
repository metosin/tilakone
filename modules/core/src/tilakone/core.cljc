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
        from-value (-> fsm ::value)
        transition (-> fsm (u/get-transition signal))
        to-state   (-> transition ::to (or from-state))
        to-value   (u/apply-actions (assoc fsm ::signal     signal
                                               ::from-state from-state
                                               ::transition transition
                                               ::to-state   to-state)
                                    from-value)]
    (cond
      (u/push-state? to-state) (-> fsm
                                   (update ::stack conj from-state)
                                   (assoc ::state (second to-state)
                                          ::value to-value))
      (u/pop-state? to-state) (assoc fsm ::state (-> fsm ::stack peek)
                                         ::stack (-> fsm ::stack pop)
                                         ::value to-value)
      :else (assoc fsm ::state to-state
                       ::value to-value))))

(defn apply-guards
  "Accepts a FSM and a signal, resolves all transitions that are possible with given
  signal, returns seq of tuples of `[transition guard-results]`, where `guard-results` is
  a seq of results reported by guards, each result is a map with `:tilakone.core/allow?`
  (boolean indicating guard verdict), `:tilakone.core/guard` (the guard data from fsm), and
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
    {::states  [{::name        Any ;                      State name (can be string, keyword, symbol, any clojure value)
                 ::desc        Str ;                      Optional state description
                 ::transitions [{::name    Any ;          Transition name
                                 ::desc    Str ;          Transition description
                                 ::to      Any ;          Name of the next state
                                 ::on      Matcher ;      Data for match?, does the signal match this transition?
                                 ::guards  [Guard] ;      Data for guard?, is this transition allowed?
                                 ::actions [Action]}] ;   Actions to be performed on this transition
                 ; Guards and actions used when state is transferred to this state:
                 ::enter       {::guards  [Guard]
                                ::actions [Action]}
                 ; Guards and actions used when state is transferred from this state:
                 ::leave       {::guards  [Guard]
                                ::actions [Action]}
                 ; Guards and actions used when state transfer is not made:
                 ::stay        {::guards  [Guard]
                                ::actions [Action]}}]
     ::state   Any ;                                      Current state
     ::value   Any ;                                      Current application specific value
     ::stack   [Any] ;                                    Stack of states:
     ::match?  (fn [signal on] ... true/false) ;          Signal matching predicate
     ; The first arg to guard? and action! functions is the `fsm`, with the `::signal` and
     ; current guard or action data.
     ::guard?  (fn [{:tilakone.core/keys [signal guard] :as fsm} value] ... true/false) ;   Guard function
     ::action! (fn [{:tilakone.core/keys [signal action] :as fsm} value] ... value)}) ;     Action function

  )
