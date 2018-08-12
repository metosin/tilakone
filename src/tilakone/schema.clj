(ns tilakone.schema
  (:require [schema.core :as s :refer [defschema]])
  (:import (clojure.lang IFn)))

;;
;; Common schemas:
;;

(def State s/Any)
(def Signal s/Any)
(def Action [s/Any])
(def Guard [s/Any])

;;
;; Transitions:
;;

(defschema SimpleTransition {:state                    State
                             (s/optional-key :actions) [Action]
                             s/Keyword                 s/Any})

(defschema GuardedTransition {Guard SimpleTransition})

(def Transition (s/conditional
                  :state SimpleTransition
                  :else GuardedTransition))

;;
;; States:
;;

(defschema States
  {State {:transitions            {Signal Transition}
          (s/optional-key :enter) [Action]
          (s/optional-key :leave) [Action]
          (s/optional-key :stay)  [Action]}})

(def states-validator (s/validator States))

(defn validate-states [states]
  (states-validator states)
  (when-let [errors (reduce (fn [errors [from-state {:keys [transitions]}]]
                              (reduce (fn [errors [signal transition]]
                                        (if (:state transition)
                                          (if (contains? states (-> transition :state))
                                            errors
                                            (cons [from-state signal (-> transition :state)] errors))
                                          (concat (->> transition
                                                       vals
                                                       (map :state)
                                                       (remove states))
                                                  errors)))
                                      errors
                                      transitions))
                            nil
                            states)]
    (throw (ex-info "unknown target states" {:errors errors})))
  states)

;;
;; FSM:
;;

(defschema FSM
  {:states    States
   :action-fn IFn
   :guard-fn  IFn
   :state     s/Any
   :value     s/Any})

(def fsm-validator (s/validator FSM))

(defn validate-fsm [fsm]
  (fsm-validator fsm)
  (validate-states (:states fsm))
  fsm)
