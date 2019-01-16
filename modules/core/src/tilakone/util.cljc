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


;;
;; Guards:
;;


(defn- try-guard [guard? ctx]
  (try
    (let [response (guard? ctx)]
      (when-not response
        {:guard  (-> ctx :guard)
         :result response}))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:guard   (-> ctx :guard)
       :result  (ex-data e)
       :message (ex-message e)})))


(defn apply-guards! [transition ctx state]
  (let [guard? (-> ctx :process :guard?)
        errors (->> transition
                    :guards
                    (reduce (fn [errors guard]
                              (if-let [result (try-guard guard? (assoc ctx :guard guard))]
                                (cons result errors)
                                errors))
                            nil)
                    (reverse)
                    (seq))]
    (when errors
      (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                              (-> state :name)
                              (-> ctx :signal pr-str))
                      {:type          :tilakone.core/error
                       :error         :tilakone.core/rejected-by-guard
                       :state         state
                       :signal        (-> ctx :signal)
                       :transition    transition
                       :value         (-> ctx :process :value)
                       :guard-results errors}))))
  transition)


;;
;; Transitions:
;;


(defn- default-match? [ctx]
  (= (-> ctx :signal)
     (-> ctx :on)))


(defn- find-transition [ctx transitions]
  (let [match? (-> ctx :process :match? (or default-match?))]
    (find-first (fn [{:keys [on]}]
                  (or (= on :tilakone.core/_)
                      (match? (assoc ctx :on on))))
                transitions)))


(defn get-transition [process state signal]
  (let [ctx        {:process process
                    :signal  signal}
        transition (find-transition ctx (-> state :transitions))]
    (when-not transition
      (or (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                                  (-> state :name)
                                  (-> signal pr-str))
                          {:type   :tilakone.core/error
                           :error  :tilakone.core/missing-transition
                           :state  state
                           :signal signal}))))
    (apply-guards! ctx process state)
    transition))


;;
;; Actions:
;;


(defn apply-process-actions [process signal actions]
  (let [action! (-> process :action!)]
    (-> (reduce (fn [ctx action]
                  (->> (assoc ctx :action action)
                       (action!)
                       (assoc-in ctx [:process :value])))
                {:process process
                 :signal  signal}
                actions)
        :process)))
