(ns tilakone.util
  "Util functions required to create an FSM")


;; ======== Generic utils ========

(defn find-first
  "First the first element in the collection which satisfies the provided predicate"
  [pred? coll]
  (some (fn [v]
          (when (pred? v)
            v))
        coll))


;; ======== State untils ========

(defn state
  "Get the state defintion"
  [fsm state-name]
  (->> fsm
       :tilakone.core/states
       (find-first #(-> % :tilakone.core/name (= state-name)))))


(defn current-state
  "Get the state definition of the current state"
  [fsm]
  (state fsm (-> fsm :tilakone.core/state)))


;; ======== Helper functions to find guards or actions ========

(defn- get-transition-fns
  "Based on the state transition get the functions to be applied"
  [fn-type fsm transition]
  (let [from (:tilakone.core/state fsm)
        to   (or (:tilakone.core/to transition)
                 from)]
    (if (= from to)
                                        ; No state change:
      (concat (fn-type transition)
              (-> (state fsm from)
                  :tilakone.core/stay
                  fn-type))
                                        ; State change:
      (concat (-> (state fsm from)
                  :tilakone.core/leave
                  fn-type)
              (fn-type transition)
              (-> (state fsm to)
                  :tilakone.core/enter
                  fn-type)))))


(def ^:private get-transition-guards
  (partial get-transition-fns :tilakone.core/guards))

(def ^:private get-transition-actions
  (partial get-transition-fns :tilakone.core/actions))


;; ======== Guard utils ========

(defn- try-guard
  "Applies guard function to fsm"
  [fsm signal guard]
  (try
    (let [guard?   (-> fsm :tilakone.core/guard?)
          response (guard? fsm signal guard)]
      (when-not response
        {:tilakone.core/guard  guard
         :tilakone.core/result response}))
    (catch clojure.lang.ExceptionInfo e
      {:tilakone.core/guard  guard
       :tilakone.core/result e})))


(defn apply-guards
  "Applies transitions functions with the guards to the state"
  [fsm signal transition]
  (->> (get-transition-guards fsm transition)
       (keep #(try-guard fsm signal %))
       (seq)))


;; ======== Transition utils ========

(defn- default-match?
  "Default state matching predicate"
  [signal on]
  (= signal on))


(defn get-transitions
  "Based on the signal get the transitions that satisfy the matching predicate"
  [fsm signal]
  (let [match? (or (:tilakone.core/match? fsm)
                   default-match?)]
    (->> fsm
         (current-state)
         :tilakone.core/transitions
         (filter (fn [{:keys [:tilakone.core/on]}]
                   (or (= on :tilakone.core/_)
                       (match? signal on))))
         (seq))))


(defn- allowed-transition?
  "Given the FSM and the signal, checks if the transitions are allowed"
  [fsm signal transition]
  (let [reject? (partial try-guard fsm signal)
        allow?  (complement reject?)
        guards  (get-transition-guards fsm transition)]
    (every? allow? guards)))


(defn- missing-transition!
  "Finds out the required transitions in the FSM and returns in the form of exception"
  [fsm signal]
  (let [state-name (-> (current-state fsm)
                       :tilakone.core/name)]
    (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                            state-name
                            (pr-str signal))
                    {:tilakone.core/type   :tilakone.core/error
                     :tilakone.core/error  :tilakone.core/missing-transition
                     :tilakone.core/state  state-name
                     :tilakone.core/signal signal}))))


(defn- none-allowed!
  "Finds out the forbidden transitions in the FSM and returns in the form of exception"
  [fsm signal]
  (let [state-name (-> (current-state fsm)
                       :tilakone.core/name)]
    (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                            state-name
                            (pr-str signal))
                    {:tilakone.core/type   :tilakone.core/error
                     :tilakone.core/error  :tilakone.core/rejected-by-guard
                     :tilakone.core/state  state-name
                     :tilakone.core/signal signal}))))


(defn get-transition
  "Returns the first allowed transition in the defined FSM. Throws and
  exception if no transition is defined or if no transition is
  allowed"
  [fsm signal]
  (let [transitions (or (get-transitions fsm signal)
                        (missing-transition! fsm signal))
        allow?      (partial allowed-transition? fsm signal)]
    (or (find-first allow? transitions)
        (none-allowed! fsm signal))))


;; ======== Action utils ========

(defn apply-actions
  "Apply actions defined in the state the fsm has transitioned into"
  [fsm signal transition]
  (let [action! (-> fsm :tilakone.core/action!)]
    (reduce (fn [fsm action]
              (action! fsm signal action))
            fsm
            (get-transition-actions fsm transition))))
