(ns tilakone.core
  (:require [tilakone.util :as u]))


(def _
  "Special symbol to denote that the transition matches any signal"
  ::_)


(defn apply-signal
  "Accepts a process and a signal, applies the signal to process and returns
  (possibly) updated process."
  [process signal]
  (let [ctx        {:process process
                    :signal  signal}
        transition (u/get-transition ctx)
        from-state (-> process :state)
        to-state   (-> transition :to (or from-state))]
    (-> ctx
        (u/apply-actions transition)
        :process
        (assoc :state to-state))))


(defn apply-guards
  "Accepts a process and a signal, resolves all transitions that are possible with given
  signal, returns seq of tuples of `[transitions guard-errors]`, where `guard-errors` are
  errors reported by guards. If none of the guards report any errors for transition then
  `guard-errors` is `nil`."
  [process signal]
  (let [ctx {:process process
             :signal  signal}]
    (->> (u/get-transitions ctx)
         (map (fn [transition]
                [transition (seq (u/apply-guards ctx transition))])))))


(defn transfers-to
  "Accepts a process and a signal, returns the name of the state the signal would
  transfer the process if applied. Returns `nil` if signal is not allowed."
  [process signal]
  (->> (apply-guards process signal)
       (u/find-first (complement second))
       first
       :to))


(comment

  ;
  ; The FSM looks like this:
  ;

  (def FSM
    {:states  [{:name        Any ;                    State name (can be string, keyword, symbol, any clojure value)
                :desc        Str ;                    State description
                :transitions [{:name    Any ;         Transition name
                               :desc    Str ;         Transition description
                               :to      Any ;         Name of the next state
                               :on      Matcher ;     Data for match?, does the signal match this transition?
                               :guards  [Guard] ;     Data for guard?, is this transition allowed?
                               :actions [Action]}] ;  Actions to be performed on this transition
                :enter       {:guards  [Guard] ;      Guards that must approve transfer to this state.
                              :actions [Action]} ;    Actions to be performed when entering this state.
                :leave       {:guards  [Guard] ;      Guards that must approve transfer from this state.
                              :actions [Action]} ;    Actions to be performed when leaving this state
                :stay        {:guards  [Guard] ;      Guards that must approve transfer staying in this.
                              :actions [Action]}}] ;  Actions to be performed when signal is processed, but state remains the same
     :match?  (fn [{:keys [process signal on]}] ... true/false) ;      Signal matching predicate
     :guard?  (fn [{:keys [process signal guard]}] ... true/false) ;   Guard matching predicate
     :action! (fn [{:keys [process signal action]}] ... value) ;       Action function, return new `value`
     :state   Any ;     Current state
     :value   Any}) ;   Current value

  )
