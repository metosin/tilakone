(ns tilakone.util)

(defn find-first [pred? coll]
  (some (fn [v]
          (when (pred? v)
            v))
        coll))

(defn get-state [fsm state-name]
  (find-first (comp (partial = state-name) :name)
              (-> fsm :states)))

(defn default-match? [_ signal on]
  (= signal on))

(defn find-transition [fsm state signal]
  (let [value  (-> fsm :value)
        match? (-> fsm :match? (or default-match?))]
    (find-first (fn [{:keys [on]}]
                  (or (= on :tilakone.core/_)
                      (match? value signal on)))
                (-> state :transitions))))

(defn try-guard [guard? value signal guard]
  (try
    (let [response (guard? value signal guard)]
      (when-not response
        {:guard  guard
         :result response}))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:guard   guard
       :result  (ex-data e)
       :message #?(:clj (.getMessage e) :cljs (str e))})))

(defn apply-guards! [transition {:keys [value guard?]} state signal]
  (let [errors (reduce (fn [errors guard]
                         (if-let [result (try-guard guard? value signal guard)]
                           (conj errors result)
                           errors))
                       []
                       (-> transition :guards))]
    (when (seq errors)
      (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                              (-> state :name)
                              (-> signal pr-str))
                      {:type          :tilakone.core/error
                       :error         :tilakone.core/rejected-by-guard
                       :state         state
                       :signal        signal
                       :transition    transition
                       :value         value
                       :guard-results errors})))
    transition))

(defn get-transition [fsm state signal]
  (-> (find-transition fsm state signal)
      (or (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                                  (-> state :name)
                                  (-> signal pr-str))
                          {:type   :tilakone.core/error
                           :error  :tilakone.core/missing-transition
                           :state  state
                           :signal signal})))
      (apply-guards! fsm state signal)))

(defn apply-actions [value action! signal actions]
  (reduce (fn [value action]
            (action! value signal action))
          value
          actions))

(defn apply-fsm-actions [fsm signal actions]
  (update fsm :value apply-actions (-> fsm :action!) signal actions))
