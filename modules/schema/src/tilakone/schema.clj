(ns tilakone.schema
  (:require [clojure.string :as str]
            [schema.core :as s :refer [defschema]]
            [tilakone.core :as tk])
  (:import (clojure.lang IFn)))


(def StateName s/Any)
(def TransitionName s/Any)
(def Action s/Any)
(def Matcher s/Any)
(def Guard s/Any)


(def state-actions-and-guards {(s/optional-key ::tk/actions) [Action]
                               (s/optional-key ::tk/guards)  [Guard]})


(defschema Transition {(s/optional-key ::tk/name)    TransitionName
                       (s/optional-key ::tk/desc)    s/Str
                       (s/optional-key ::tk/to)      StateName
                       ::tk/on                       Matcher
                       (s/optional-key ::tk/guards)  [Guard]
                       (s/optional-key ::tk/actions) [Action]
                       s/Keyword                     s/Any})


(defschema State {::tk/name                   StateName
                  (s/optional-key ::tk/desc)  s/Str
                  ::tk/transitions            [Transition]
                  (s/optional-key ::tk/enter) state-actions-and-guards
                  (s/optional-key ::tk/stay)  state-actions-and-guards
                  (s/optional-key ::tk/leave) state-actions-and-guards
                  s/Keyword                   s/Any})


(defschema FSM {::tk/states                   [State]
                ::tk/state                    StateName
                (s/optional-key ::tk/match?)  IFn
                (s/optional-key ::tk/guard?)  IFn
                (s/optional-key ::tk/action!) IFn
                s/Keyword                     s/Any})


(defn validate-states [states]
  (let [known-state? (->> states
                          (map ::tk/name)
                          (set))
        errors       (mapcat (fn [state]
                               (->> state
                                    ::tk/transitions
                                    (remove (fn [{::tk/keys [to]}]
                                              (or (nil? to)
                                                  (known-state? to))))
                                    (map (fn [transition]
                                           {::tk/state      state
                                            ::tk/transition transition
                                            :message        (format "state [%s] has transition [%s] to unknown state [%s]"
                                                                    (-> state ::tk/name)
                                                                    (-> transition ::tk/name (or "anonymous"))
                                                                    (-> transition ::tk/to))}))))
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
  (validate-states (::tk/states fsm))
  fsm)
