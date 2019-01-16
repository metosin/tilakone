(ns tilakone.core
  (:require [tilakone.util :as u]))


(def _
  "Special symbol to denote that the transition matches any signal"
  ::_)


(defn apply-signal
  "Accepts a process and a signal, applies the signal to process and returns
  (possibly) updated process."
  [process signal]
  (let [current-state-name (-> process :state)
        current-state      (u/get-process-state process current-state-name)
        transition         (u/get-transition process current-state signal)
        next-state-name    (-> transition :to (or current-state-name))
        next-state         (u/get-process-state process next-state-name)
        ctx                {:process    process
                            :signal     signal
                            :from-state current-state-name
                            :to-state   next-state-name}]
    (if (= next-state-name current-state-name)
      ; signal does not cause state change:
      (-> ctx
          (u/apply-guards (-> current-state :stay :guards))
          (u/report-guard-errors!)
          (u/apply-actions (-> transition :actions))
          (u/apply-actions (-> current-state :stay :actions))
          :process)
      ; signal does cause state change from current-state to next-state:
      (-> ctx
          (u/apply-guards (-> current-state :leave :guards))
          (u/apply-guards (-> transition :guards))
          (u/apply-guards (-> next-state :enter :guards))
          (u/report-guard-errors!)
          (u/apply-actions (-> current-state :leave :actions))
          (update :process assoc :state next-state-name)
          (u/apply-actions (-> transition :actions))
          (u/apply-actions (-> next-state :enter :actions))
          :process))))


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
                               :on      Matcher ;     Data for match?, does the sfignal match this transition?
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
