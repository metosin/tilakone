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


(defn get-process-state [process state-name]
  (->> process
       :tilakone.core/states
       (find-first #(-> % :tilakone.core/name (= state-name)))))


(defn get-process-current-state [process]
  (get-process-state process (-> process :tilakone.core/state)))


;;
;; Helper to find guards or actions:
;;

(defn- get-transition-fns [fn-type ctx transition]
  (let [process (-> ctx :tilakone.core/process)
        from    (-> process :tilakone.core/state)
        to      (-> transition :tilakone.core/to (or from))]
    (if (= from to)
      ; No state change:
      (concat (-> transition fn-type)
              (-> process (get-process-state from) :tilakone.core/stay fn-type))
      ; State change:
      (concat (-> process (get-process-state from) :tilakone.core/leave fn-type)
              (-> transition fn-type)
              (-> process (get-process-state to) :tilakone.core/enter fn-type)))))


(def ^:private get-transition-guards (partial get-transition-fns :tilakone.core/guards))
(def ^:private get-transition-actions (partial get-transition-fns :tilakone.core/actions))

;;
;; Guards:
;;


(defn- try-guard [guard? ctx guard]
  (try
    (let [response (guard? (assoc ctx :tilakone.core/guard guard))]
      (when-not response
        {:tilakone.core/guard  guard
         :tilakone.core/result response}))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:tilakone.core/guard  guard
       :tilakone.core/result e})))


(defn apply-guards [ctx transition]
  (let [try-guard (partial try-guard (-> ctx :tilakone.core/process :tilakone.core/guard?) ctx)]
    (->> (get-transition-guards ctx transition)
         (keep try-guard))))


;;
;; Transitions:
;;


(defn- default-match? [ctx]
  (= (-> ctx :tilakone.core/signal)
     (-> ctx :tilakone.core/on)))


(defn get-transitions [ctx]
  (let [match? (-> ctx :tilakone.core/process :tilakone.core/match? (or default-match?))]
    (->> ctx
         :tilakone.core/process
         (get-process-current-state)
         :tilakone.core/transitions
         (filter (fn [{:tilakone.core/keys [on]}]
                   (or (= on :tilakone.core/_)
                       (match? (assoc ctx :tilakone.core/on on)))))
         (seq))))


(defn- allowed-transition? [ctx transition]
  (let [reject? (partial try-guard (-> ctx :tilakone.core/process :tilakone.core/guard?) ctx)
        allow?  (complement reject?)
        guards  (get-transition-guards ctx transition)]
    (every? allow? guards)))


(defn- missing-transition! [ctx]
  (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                          (-> ctx :tilakone.core/process (get-process-current-state) :tilakone.core/name)
                          (-> ctx :tilakone.core/signal pr-str))
                  {:tilakone.core/type   :tilakone.core/error
                   :tilakone.core/error  :tilakone.core/missing-transition
                   :tilakone.core/state  (-> ctx :tilakone.core/process (get-process-current-state) :tilakone.core/name)
                   :tilakone.core/signal (-> ctx :tilakone.core/signal)})))


(defn- none-allowed! [ctx]
  (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                          (-> ctx :tilakone.core/process (get-process-current-state) :tilakone.core/name)
                          (-> ctx :tilakone.core/signal pr-str))
                  {:tilakone.core/type   :tilakone.core/error
                   :tilakone.core/error  :tilakone.core/rejected-by-guard
                   :tilakone.core/state  (-> ctx :tilakone.core/process (get-process-current-state))
                   :tilakone.core/signal (-> ctx :tilakone.core/signal)})))


(defn get-transition [ctx]
  (let [transitions (or (get-transitions ctx)
                        (missing-transition! ctx))
        allow?      (partial allowed-transition? ctx)]
    (or (find-first allow? transitions)
        (none-allowed! ctx))))


;;
;; Actions:
;;


(defn apply-actions [ctx transition]
  (let [action! (-> ctx :tilakone.core/process :tilakone.core/action!)
        actions (get-transition-actions ctx transition)]
    (reduce (fn [ctx action]
              (->> (assoc ctx :tilakone.core/action action)
                   (action!)))
            ctx
            actions)))
