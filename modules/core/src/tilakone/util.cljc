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
       :states
       (find-first #(-> % :name (= state-name)))))


(defn current-state [fsm]
  (state fsm (-> fsm :state)))


;;
;; Helper to find guards or actions:
;;

(defn- get-transition-fns [fn-type fsm transition]
  (let [from (-> fsm :state)
        to   (-> transition :to (or from))]
    (if (= from to)
      ; No state change:
      (concat (-> transition fn-type)
              (-> fsm (state from) :stay fn-type))
      ; State change:
      (concat (-> fsm (state from) :leave fn-type)
              (-> transition fn-type)
              (-> fsm (state to) :enter fn-type)))))


(def ^:private get-transition-guards (partial get-transition-fns :guards))
(def ^:private get-transition-actions (partial get-transition-fns :actions))

;;
;; Guards:
;;


(defn- try-guard [fsm signal guard]
  (try
    (let [guard?   (-> fsm :guard?)
          response (guard? fsm signal guard)]
      (when-not response
        {:guard  guard
         :result response}))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs js/Error) e
      {:guard  guard
       :result e})))


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
  (let [match? (-> fsm :match? (or default-match?))]
    (->> fsm
         (current-state)
         :transitions
         (filter (fn [{:keys [on]}]
                   (or (= on :_)
                       (match? signal on))))
         (seq))))


(defn- allowed-transition? [fsm signal transition]
  (let [reject? (partial try-guard fsm signal)
        allow?  (complement reject?)
        guards  (get-transition-guards fsm transition)]
    (every? allow? guards)))


(defn- missing-transition! [fsm signal]
  (throw (ex-info (format "missing transition from state [%s] with signal [%s]"
                          (-> fsm (current-state) :name)
                          (-> signal pr-str))
                  {:type   :error
                   :error  :missing-transition
                   :state  (-> fsm (current-state) :name)
                   :signal signal})))


(defn- none-allowed! [fsm signal]
  (throw (ex-info (format "transition from state [%s] with signal [%s] forbidden by guard(s)"
                          (-> fsm (current-state) :name)
                          (-> signal pr-str))
                  {:type   :error
                   :error  :rejected-by-guard
                   :state  (-> fsm (current-state))
                   :signal signal})))


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
  (let [action! (-> fsm :action!)]
    (reduce (fn [fsm action]
              (action! fsm signal action))
            fsm
            (get-transition-actions fsm transition))))
