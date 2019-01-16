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
       :states
       (find-first #(-> % :name (= state-name)))))


(defn get-process-current-state [process]
  (get-process-state process (-> process :state)))


;;
;; Helper to find guards or actions:
;;

(defn- get-transition-fns [fn-type ctx transition]
  (let [process (-> ctx :process)
        from    (-> process :state)
        to      (-> transition :to (or from))]
    (if (= from to)
      ; No state change:
      (concat (-> transition fn-type)
              (-> process (get-process-state from) :stay fn-type))
      ; State change:
      (concat (-> process (get-process-state from) :leave fn-type)
              (-> transition fn-type)
              (-> process (get-process-state to) :enter fn-type)))))


(def ^:private get-transition-guards (partial get-transition-fns :guards))
(def ^:private get-transition-actions (partial get-transition-fns :actions))

;;
;; Guards:
;;


(defn- try-guard [guard? ctx guard]
  (try
    (let [response (guard? (assoc ctx :guard guard))]
      (when-not response
        {:guard  guard
         :result response}))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:guard  guard
       :result e})))


(defn apply-guards [ctx transition]
  (let [try-guard (partial try-guard (-> ctx :process :guard?) ctx)]
    (->> (get-transition-guards ctx transition)
         (keep try-guard))))


;;
;; Transitions:
;;


(defn- default-match? [ctx]
  (= (-> ctx :signal)
     (-> ctx :on)))


(defn get-transitions [ctx]
  (let [match? (-> ctx :process :match? (or default-match?))]
    (->> ctx
         :process
         (get-process-current-state)
         :transitions
         (filter (fn [{:keys [on]}]
                   (or (= on :tilakone.core/_)
                       (match? (assoc ctx :on on)))))
         (seq))))


(defn- allowed-transition? [ctx transition]
  (let [reject? (partial try-guard (-> ctx :process :guard?) ctx)
        allow?  (complement reject?)
        guards  (get-transition-guards ctx transition)]
    (every? allow? guards)))


(defn- missing-transition! [ctx]
  (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                          (-> ctx :process (get-process-current-state) :name)
                          (-> ctx :signal pr-str))
                  {:type   :tilakone.core/error
                   :error  :tilakone.core/missing-transition
                   :state  (-> ctx :process (get-process-current-state) :name)
                   :signal (-> ctx :signal)})))


(defn- none-allowed! [ctx]
  (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                          (-> ctx :process (get-process-current-state) :name)
                          (-> ctx :signal pr-str))
                  {:type   :tilakone.core/error
                   :error  :tilakone.core/rejected-by-guard
                   :state  (-> ctx :process (get-process-current-state))
                   :signal (-> ctx :signal)})))


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
  (let [action! (-> ctx :process :action!)
        actions (get-transition-actions ctx transition)]
    (reduce (fn [ctx action]
              (->> (assoc ctx :action action)
                   (action!)
                   (update ctx :process assoc :value)))
            ctx
            actions)))
