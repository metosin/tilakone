(ns tilakone.util
  "Tilakone implementation, consider this as private api that can change any time.")

(defn find-first [pred? coll]
  (some (fn [v]
          (when (pred? v)
            v))
        coll))

(defn get-process-state [process state-name]
  (find-first (comp (partial = state-name) :name)
              (-> process :states)))

(defn default-match? [_ signal on]
  (= signal on))

(defn find-transition [process state signal]
  (let [value  (-> process :value)
        match? (-> process :match? (or default-match?))]
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
       :message (ex-message e)})))

(defn apply-guards! [transition {:keys [value guard?]} state signal]
  (let [errors (reduce (fn [errors guard]
                         (if-let [result (try-guard guard? value signal guard)]
                           (conj errors result)
                           errors))
                       []
                       (-> transition :guards))]
    (when (seq errors)
      (throw (ex-info (str "transition from state [" (-> state :name) "] with signal [" (-> signal pr-str) "] forbidden by guard(s)")
                      {:type          :tilakone.core/error
                       :error         :tilakone.core/rejected-by-guard
                       :state         state
                       :signal        signal
                       :transition    transition
                       :value         value
                       :guard-results errors})))
    transition))

(defn get-transition [process state signal]
  (-> (find-transition process state signal)
      (or (throw (ex-info (str "missing transition from state [" (-> state :name) "] with signal [" (-> signal pr-str) "]")
                          {:type   :tilakone.core/error
                           :error  :tilakone.core/missing-transition
                           :state  state
                           :signal signal})))
      (apply-guards! process state signal)))

(defn apply-actions [value action! signal actions]
  (reduce (fn [value action]
            (action! value signal action))
          value
          actions))

(defn apply-process-actions [process signal actions]
  (update process :value apply-actions (-> process :action!) signal actions))
