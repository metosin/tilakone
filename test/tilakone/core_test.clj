(ns tilakone.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :refer :all]))

; Example state machine from https://github.com/cdorrat/reduce-fsm#basic-fsm:

(def count-ab-fsm
  {:states    {:start   {:transitions {\a {:state :found-a}
                                       _ {:state :start}}}
               :found-a {:transitions {\a {:state :found-a}
                                       \b {:state   :start
                                           :actions [[:inc-val]]}
                                       _  {:state :start}}}}
   :action-fn (fn [action value & _]
                (case action
                  :inc-val (inc value)))
   :state     :start
   :value     0})

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
