(ns tilakone.schema
  (:require [clojure.string :as str]
            [schema.core :as s :refer [defschema]])
  (:import (clojure.lang IFn)))

(comment

  ; Mental map for FMS schema:

  {:states  [{:name        Any ; State name (can be string, keyword, symbol, any clojure value)
              :desc        Str ; State description
              :transitions [{:name    Any ; Transition name
                             :desc    Str ; Transition description
                             :to      Any ; Name of the next state
                             :on      Matcher ; Data for match?, does the signal match this transition?
                             :guards  [Guard] ; Data for guard?, is this transition allowed?
                             :actions [Action]}] ; Actions to be performed on this transition
              :enter       [Action] ; Actions to be performed when entering this state
              :leave       [Action] ; Actions to be performed when leaving this state
              :stay        [Action]}] ; Actions to be performed when signal is processed, but state remains the same
   :match?  (fn [value signal & matcher] ... true/false) ; Signal matching predicate
   :guard?  (fn [value signal & guard] ... true/false) ; Guard matching predicate
   :action! (fn [value signal & action] ... value) ; Action function
   :state   Any ; Current state
   :value   Any} ; Current value

  )

(def StateName s/Any)
(def TransitionName s/Any)
(def Action s/Any)
(def Matcher s/Any)
(def Guard s/Any)

(defschema Transition {(s/optional-key :name)    TransitionName
                       (s/optional-key :desc)    s/Str
                       (s/optional-key :to)      StateName
                       :on                       Matcher
                       (s/optional-key :guards)  [Guard]
                       (s/optional-key :actions) [Action]
                       s/Keyword                 s/Any})

(defschema State {:name                   StateName
                  (s/optional-key :desc)  s/Str
                  :transitions            [Transition]
                  (s/optional-key :enter) [Action]
                  (s/optional-key :stay)  [Action]
                  (s/optional-key :leave) [Action]
                  s/Keyword               s/Any})

(defschema FSM {:states                   [State]
                :state                    StateName
                :value                    s/Any
                (s/optional-key :match?)  IFn
                (s/optional-key :guard?)  IFn
                (s/optional-key :action!) IFn
                s/Keyword                 s/Any})

(defn validate-states [states]
  (let [known-state? (->> states
                          (map :name)
                          (set))
        errors       (mapcat (fn [state]
                               (->> state
                                    :transitions
                                    (remove (comp known-state? :to))
                                    (map (fn [transition]
                                           {:state      state
                                            :transition transition
                                            :message    (format "state [%s] has transition [%s] to unknown state [%s]"
                                                                (-> state :name)
                                                                (-> transition :name (or "anonymous"))
                                                                (-> transition :to))}))))
                             states)]
    (when (seq errors)
      (throw (ex-info (str "unknown target states: " (->> errors
                                                          (map :message)
                                                          (str/join ", ")))
                      {:type   :tilakone.core/error
                       :errors errors}))))
  states)

(def fsm-checker (s/checker FSM))

(defn validate-fsm [fsm]
  (when-let [schema-errors (fsm-checker fsm)]
    (throw (ex-info "FSM does not match schema"
                    {:type          :tilakone.core/error
                     :error         :tilakone.core/schema-error
                     :schema-errors schema-errors
                     :fsm           fsm})))
  (validate-states (:states fsm))
  fsm)
