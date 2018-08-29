(ns tilakone.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :refer :all]))

; Example state machine from https://github.com/cdorrat/reduce-fsm#basic-fsm:
;
; (defn inc-val [val & _] (inc val))
;
; (fsm/defsm count-ab
;   [[:start
;     \a -> :found-a]
;    [:found-a
;     \a ->  :found-a
;     \b -> {:action inc-val} :start
;     _ -> :start]])

(def count-ab-states
  [{:name        :start
    :transitions [{:on \a
                   :to :found-a}
                  {:on _
                   :to :start}]}
   {:name        :found-a
    :transitions [{:on \a
                   :to :found-a}
                  {:on      \b
                   :to      :start
                   :actions [:inc-val]}
                  {:on _
                   :to :start}]}])

(def count-ab-fsm
  {:states  count-ab-states
   :action! (fn [value signal action]
              (case action
                :inc-val (inc value)))
   :state   :start
   :value   0})

;;
;; Tests:
;;

(deftest apply-signal-test
  (fact
    (-> count-ab-fsm
        (apply-signal \a))
    => {:state :found-a
        :value 0})

  (fact
    (-> count-ab-fsm
        (apply-signal \a)
        (apply-signal \a))
    => {:state :found-a
        :value 0})

  (fact
    (-> count-ab-fsm
        (apply-signal \a)
        (apply-signal \a)
        (apply-signal \b))
    => {:state :start
        :value 1})

  (fact
    (reduce apply-signal
            count-ab-fsm
            "abaaabc")
    => {:value 2}))

(deftest apply-signal-example-test

  ; Sample input from reduce-fsm:
  ;   (map (partial count-ab 0) ["abaaabc" "aaacb" "bbbcab"])
  ;   => (2 0 1)

  (fact
    (->> ["abaaabc" "aaacb" "bbbcab"]
         (map (partial reduce apply-signal count-ab-fsm))
         (map :value))
    => [2 0 1])
  )
