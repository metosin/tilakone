(ns tilakone.util
  (:require [clojure.string :as str]))

(defn message->str [message]
  (if (string? message)
    message
    (->> message
         (map (fn [s]
                (if (string? s)
                  s
                  (pr-str s))))
         (str/join " "))))

(defn error! [message data]
  (throw (ex-info (message->str message)
                  (merge {:type :tilakone.core/error} data))))

(defn simple-transition? [transitions]
  (and (map? transitions)
       (contains? transitions :state)))

(defn missing-transition! [fsm signal]
  (error! ["missing transition from" (-> fsm :state) "with signal" signal]
          {:signal signal
           :fsm    fsm}))

(defn missing-guarded-transition! [fsm signal]
  (error! ["missing guarded transition from" (-> fsm :state)
           "with signal" signal
           "with value" (-> fsm :value)]
          {:signal signal
           :fsm    fsm}))

(defn guard-matcher [{:keys [value guard-fn]}]
  (fn [[guard transition]]
    (when (or (= guard :tilakone.core/_)
              (apply guard-fn (first guard) value (rest guard)))
      transition)))

(defn get-transition [fsm state signal]
  (let [transitions (-> state :transitions)
        transition (or (get transitions signal)
                       (get transitions :tilakone.core/_)
                       (missing-transition! fsm signal))]
    (if (simple-transition? transition)
      transition
      (or (some (guard-matcher fsm) transition)
          (missing-guarded-transition! fsm signal)))))

(defn apply-actions [{:keys [action-fn value] :as fsm} actions]
  (assoc fsm :value (reduce (fn [value action]
                              (apply action-fn (first action) value (rest action)))
                            value
                            actions)))
