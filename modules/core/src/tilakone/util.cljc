(ns tilakone.util)


;;
;; Generic utils:
;;


(defn find-first [pred? coll]
  (some (fn [v]
          (when (pred? v)
            v))
        coll))


;;
;; State:
;;


(defn state [fsm state-name]
  (->> fsm
       :tilakone.core/states
       (find-first #(-> % :tilakone.core/name (= state-name)))))


(defn current-state [fsm]
  (state fsm (-> fsm :tilakone.core/state)))


;;
;; Helper to find guards or actions:
;;

(defn- get-transition-fns [fn-type fsm transition]
  (let [from (-> fsm :tilakone.core/state)
        to   (-> transition :tilakone.core/to (or from))]
    (if (= from to)
      ; No state change:
      (concat (-> transition fn-type)
              (-> fsm (state from) :tilakone.core/stay fn-type))
      ; State change:
      (concat (-> fsm (state from) :tilakone.core/leave fn-type)
              (-> transition fn-type)
              (-> fsm (state to) :tilakone.core/enter fn-type)))))


(def ^:private get-transition-guards (partial get-transition-fns :tilakone.core/guards))
(def ^:private get-transition-actions (partial get-transition-fns :tilakone.core/actions))

;;
;; Guards:
;;


(defn- try-guard [fsm signal guard]
  (try
    (let [guard?   (-> fsm :tilakone.core/guard?)
          response (guard? fsm signal guard)]
      (when-not response
        {:tilakone.core/guard  guard
         :tilakone.core/result response}))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:tilakone.core/guard  guard
       :tilakone.core/result e})))


(defn apply-guards [fsm signal transition]
  (->> (get-transition-guards fsm transition)
       (keep #(try-guard fsm signal %))
       (seq)))


;;
;; Transitions:
;;


(defn- default-match? [signal on]
  (= signal on))


(defn get-transitions [fsm signal]
  (let [match? (-> fsm :tilakone.core/match? (or default-match?))]
    (->> fsm
         (current-state)
         :tilakone.core/transitions
         (filter (fn [{:tilakone.core/keys [on]}]
                   (or (= on :tilakone.core/_)
                       (match? signal on))))
         (seq))))


(defn- allowed-transition? [fsm signal transition]
  (let [reject? (partial try-guard fsm signal)
        allow?  (complement reject?)
        guards  (get-transition-guards fsm transition)]
    (every? allow? guards)))


(defn- missing-transition! [fsm signal]
  (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                          (-> fsm (current-state) :tilakone.core/name)
                          (-> signal pr-str))
                  {:tilakone.core/type   :tilakone.core/error
                   :tilakone.core/error  :tilakone.core/missing-transition
                   :tilakone.core/state  (-> fsm (current-state) :tilakone.core/name)
                   :tilakone.core/signal signal})))


(defn- none-allowed! [fsm signal]
  (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                          (-> fsm (current-state) :tilakone.core/name)
                          (-> signal pr-str))
                  {:tilakone.core/type   :tilakone.core/error
                   :tilakone.core/error  :tilakone.core/rejected-by-guard
                   :tilakone.core/state  (-> fsm (current-state))
                   :tilakone.core/signal signal})))


(defn get-transition [fsm signal]
  (let [transitions (or (get-transitions fsm signal)
                        (missing-transition! fsm signal))
        allow?      (partial allowed-transition? fsm signal)]
    (or (find-first allow? transitions)
        (none-allowed! fsm signal))))


;;
;; Actions:
;;


(defn apply-actions [fsm signal transition]
  (let [action! (-> fsm :tilakone.core/action!)]
    (reduce (fn [fsm action]
              (action! fsm signal action))
            fsm
            (get-transition-actions fsm transition))))
