(ns tilakone.util
    #?@(:cljs
        [(:require [goog.string :as gstring]
                   [goog.string.format])]))


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


(defn- try-guard [fsm guard]
  (try
    (let [guard?   (-> fsm :tilakone.core/guard?)
          response (-> fsm
                       (assoc :tilakone.core/guard guard)
                       (guard?))]
      {:tilakone.core/allow? (if response true false)
       :tilakone.core/guard  guard
       :tilakone.core/result response})
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:tilakone.core/allow? false
       :tilakone.core/guard  guard
       :tilakone.core/result e})))


(defn apply-guards [fsm signal transition]
  (let [fsm (assoc fsm :tilakone.core/signal signal)]
    (->> (get-transition-guards fsm transition)
         (map #(try-guard fsm %))
         (seq))))


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
  (let [fsm    (assoc fsm :tilakone.core/signal signal)
        allow? (fn [guard] (-> (try-guard fsm guard) :tilakone.core/allow?))
        guards (get-transition-guards fsm transition)]
    (every? allow? guards)))


(defn- missing-transition! [fsm signal]
  (throw (ex-info #?(:clj  (format "missing transition from state [%s] with signal [%s]"
                                  (-> fsm (current-state) :tilakone.core/name)
                                  (-> signal pr-str))
                     :cljs (gstring/format "missing transition from state [%s] with signal [%s]"
                                   (-> fsm (current-state) :tilakone.core/name)
                                   (-> signal pr-str)))
                  {:tilakone.core/type   :tilakone.core/error
                   :tilakone.core/error  :tilakone.core/missing-transition
                   :tilakone.core/state  (-> fsm (current-state) :tilakone.core/name)
                   :tilakone.core/signal signal})))


(defn- none-allowed! [fsm signal]
  (throw (ex-info #?(:clj  (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                                  (-> fsm (current-state) :tilakone.core/name)
                                  (-> signal pr-str))
                     :cljs (gstring/format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                                   (-> fsm (current-state) :tilakone.core/name)
                                   (-> signal pr-str)))
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
  (let [action-executor (-> fsm :tilakone.core/action!)]
    (-> (reduce (fn [fsm action]
                  (action-executor (assoc fsm :tilakone.core/action action)))
                (assoc fsm :tilakone.core/signal signal)
                (get-transition-actions fsm transition))
        (dissoc :tilakone.core/signal)
        (dissoc :tilakone.core/action))))
