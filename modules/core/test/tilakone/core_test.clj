(ns tilakone.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as tk]))

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

(def count-ab
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

(def count-ab-process
  {:states  count-ab
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
    (-> count-ab-process
        (tk/apply-signal \a))
    => {:state :found-a
        :value 0})

  (fact
    (-> count-ab-process
        (tk/apply-signal \a)
        (tk/apply-signal \a))
    => {:state :found-a
        :value 0})

  (fact
    (-> count-ab-process
        (tk/apply-signal \a)
        (tk/apply-signal \a)
        (tk/apply-signal \b))
    => {:state :start
        :value 1})

  (fact
    (reduce tk/apply-signal
            count-ab-process
            "abaaabc")
    => {:value 2}))

(deftest apply-signal-example-test

  ; Sample input from reduce-fsm:
  ;   (map (partial count-ab 0) ["abaaabc" "aaacb" "bbbcab"])
  ;   => (2 0 1)

  (fact
    (->> ["abaaabc" "aaacb" "bbbcab"]
         (map (partial reduce tk/apply-signal count-ab-process))
         (map :value))
    => [2 0 1])
  )


(deftest guard-test
  (testing "guards are called"
    (let [process  {:states [{:name        :init
                              :transitions [{:on     :next
                                             :to     :next
                                             :guards [:guard-1 :guard-2]}]}
                             {:name :next}]
                    :state  :init
                    :value  (atom [])
                    :guard? (fn [value _ guard]
                              (swap! value conj guard)
                              true)}
          process' (tk/apply-signal process :next)]
      (fact
        (-> process' :value deref) => [:guard-1 :guard-2])))

  (testing "guard can prevent transition"
    (let [process {:states [{:name        :init
                             :transitions [{:on     :next
                                            :to     :next
                                            :guards [:ok :fail-1 :fail-2]}]}
                            {:name :next}]
                   :state  :init
                   :value  nil
                   :guard? (fn [_ _ guard]
                             (= guard :ok))}]
      (fact
        (tk/apply-signal process :next)
        =throws=> (throws-ex-info any {:type          :tilakone.core/error
                                       :error         :tilakone.core/rejected-by-guard
                                       :state         {:name :init}
                                       :signal        :next
                                       :transition    map?
                                       :value         nil
                                       :guard-results [{:guard :fail-1, :result false}
                                                       {:guard :fail-2, :result false}]})))))


(deftest allowed?-test
  (let [process {:states  [{:name        :init
                            :transitions [{:on      :inc
                                           :guards  [[:max-val 2]]
                                           :actions [[:inc-val 1]]}]}]
                 :state   :init
                 :value   0
                 :guard?  (fn [value _ [guard-id guard-arg]]
                            (case guard-id
                              :max-val (<= value guard-arg)))
                 :action! (fn [value _ [action-id action-arg]]
                            (case action-id
                              :inc-val (+ value action-arg)))}]
    (fact
      (tk/allowed? process :foo) => falsey)

    (fact
      (-> process
          (tk/allowed? :inc))
      => truthy)

    (fact
      (-> process
          (tk/apply-signal :inc)
          (tk/allowed? :inc))
      => truthy)

    (fact
      (-> process
          (tk/apply-signal :inc)
          (tk/apply-signal :inc)
          (tk/allowed? :inc))
      => truthy)

    (fact
      (-> process
          (tk/apply-signal :inc)
          (tk/apply-signal :inc)
          (tk/apply-signal :inc)
          (tk/allowed? :inc))
      => falsey)))


(deftest match?-test
  (let [process {:states [{:name        :init
                           :transitions [{:on [:next "hello"]
                                          :to :next}]}
                          {:name :next}]
                 :state  :init
                 :match? (fn [_ signal on]
                           (= (first signal) (first on)))}]
    (fact
      (tk/allowed? process [:next "world"]) => truthy)
    (fact
      (-> process
          (tk/apply-signal [:next "!"])
          :state)
      => :next)))
