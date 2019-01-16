(ns tilakone.util)


;;
;; Generic utils:
;;


(defn- find-first [pred? coll]
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


(defn- append-guard-errors [errors ctx guards]
  (let [guard? (-> ctx :process :guard?)]
    (reduce (fn [errors guard]
              (if-let [result (try-guard guard? ctx guard)]
                (cons result errors)
                errors))
            errors
            guards)))


(defn apply-guards [ctx guards]
  (update ctx ::guards-errors append-guard-errors ctx guards))


(defn report-guard-errors! [ctx]
  (when-let [errors (some-> ctx ::guards-errors (reverse))]
    (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                            (-> ctx :process (get-process-current-state) :name)
                            (-> ctx :signal pr-str))
                    {:type          :tilakone.core/error
                     :error         :tilakone.core/rejected-by-guard
                     :state         (-> ctx :process (get-process-current-state))
                     :signal        (-> ctx :signal)
                     :guard-results errors})))
  ctx)


;;
;; Transitions:
;;


(defn- default-match? [ctx]
  (= (-> ctx :signal)
     (-> ctx :on)))


(defn get-transition [process state signal]
  (let [match? (-> process :match? (or default-match?))
        ctx    {:process process
                :signal  signal}]
    (or (->> state
             :transitions
             (find-first (fn [{:keys [on]}]
                           (or (= on :tilakone.core/_)
                               (match? (assoc ctx :on on))))))
        (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                                (-> state :name)
                                (-> signal pr-str))
                        {:type   :tilakone.core/error
                         :error  :tilakone.core/missing-transition
                         :state  state
                         :signal signal})))))


;;
;; Actions:
;;


(defn apply-actions [ctx actions]
  (let [action! (-> ctx :process :action!)]
    (reduce (fn [ctx action]
              (->> (assoc ctx :action action)
                   (action!)
                   (update ctx :process assoc :value)))
            ctx
            actions)))
