(ns tilakone.schema
  (:require [schema.core :as s :refer [defschema]])
  (:import (clojure.lang IFn)))

;;
;; Common schemas:
;;

(def State s/Any)
(def Signal s/Any)
(def Action [s/Any])
(def Guard (s/conditional
             vector? [s/Any]
             keyword? (s/pred (partial = :tilakone.core/_) '_)))

;;
;; Transitions:
;;

(defschema SimpleTransition {:to                       State
                             (s/optional-key :actions) [Action]
                             s/Keyword                 s/Any})

(defn simple-transition? [v]
  (and (map? v)
       (contains? v :to)))

(defschema GuardedTransition [(s/one Guard 'guard) (s/one SimpleTransition 'transition)])

(def GuardedTransitions (s/pred (fn [v]
                                  (and (vector? v)
                                       (even? (count v))
                                       (every? (partial s/validate GuardedTransition) (partition 2 v))))
                                'GuardedTransitions))

(def Transition (s/conditional
                  simple-transition? SimpleTransition
                  :else GuardedTransitions))

(defschema Transitions {Signal Transition})

;;
;; States:
;;

(defschema States
  {State {:transitions            Transitions
          (s/optional-key :enter) [Action]
          (s/optional-key :leave) [Action]
          (s/optional-key :stay)  [Action]}})

(def states-validator (s/validator States))

(defn validate-states [states]
  (states-validator states)
  (let [errors (reduce (fn [errors [from-state {:keys [transitions]}]]
                         (reduce (fn [errors [signal transition]]
                                   (if (simple-transition? transition)
                                     (if (contains? states (-> transition :to))
                                       errors
                                       (cons [from-state signal (-> transition :to)] errors))
                                     (concat (->> transition
                                                  (partition 2)
                                                  (map (comp :to second))
                                                  (remove states))
                                             errors)))
                                 errors
                                 transitions))
                       nil
                       states)]
    (when (seq errors)
      (throw (ex-info "unknown target states" {:errors errors}))))
  states)

;;
;; FSM:
;;

(defschema FSM
  {:states  States
   :action! IFn
   :guard?  IFn
   :match?  IFn
   :state   s/Any
   :value   s/Any})

(def fsm-validator (s/validator FSM))

(defn validate-fsm [fsm]
  (fsm-validator fsm)
  (validate-states (:states fsm))
  fsm)
