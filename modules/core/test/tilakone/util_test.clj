(ns tilakone.util-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.util :refer :all]))

; can't require tilakone.core, that would be circular dependency:
(def _ :tilakone.core/_)

(deftest find-first-test
  (fact
    (find-first some? [nil 42 nil]) => 42)
  (fact
    (find-first some? [nil nil]) => nil))

(deftest get-state-test
  (fact
    (get-process-state {:states [{:name :foo}
                                 {:name :bar}
                                 {:name :boz}]}
                       :bar)
    => {:name :bar}))

(def process {:states [{:name        :foo
                        :transitions [{:to :a
                                       :on \a}
                                      {:to :b
                                       :on \b}
                                      {:to :c
                                       :on _}]}
                       {:name        :a
                        :transitions [{:to :a
                                       :on \a}
                                      {:to :b
                                       :on \b}]}]})

(deftest find-transition-test
  (fact
    (find-transition process
                     (get-process-state process :foo)
                     \a)
    => {:to :a})
  (fact
    (find-transition process
                     (get-process-state process :foo)
                     \b)
    => {:to :b})
  (fact
    (find-transition process
                     (get-process-state process :foo)
                     \x)
    => {:to :c}))

(deftest apply-guards!-test
  (fact
    (apply-guards! {:guards [1 2 3]}
                   {:value  1
                    :guard? (constantly true)}
                   ::state
                   ::signal)
    => truthy)
  (fact
    (apply-guards! {:guards [1 2 3]}
                   {:value  1
                    :guard? (fn [value signal guard]
                              (case guard
                                1 true
                                2 false
                                3 (throw (ex-info "fail" {:rejected :reason}))))}
                   {:name :state-x}
                   :some-signal)
    =throws=> (throws-ex-info "transition from state [:state-x] with signal [:some-signal] forbidden by guard(s)"
                              {:type          :tilakone.core/error
                               :error         :tilakone.core/rejected-by-guard
                               :state         {:name :state-x}
                               :signal        :some-signal
                               :transition    {:guards [1 2 3]}
                               :value         1
                               :guard-results [{:guard  2
                                                :result false}
                                               {:guard   3
                                                :result  {:rejected :reason}
                                                :message "fail"}]})))

(deftest get-transition-test
  (fact
    (get-transition process
                    (get-process-state process :a)
                    \a)
    => {:to :a})
  (fact
    (get-transition process
                    (get-process-state process :a)
                    \b)
    => {:to :b})
  (fact
    (get-transition process
                    (get-process-state process :a)
                    \c)
    =throws=> (throws-ex-info "missing transition from state [:a] with signal [\\c]"
                              {:type   :tilakone.core/error
                               :error  :tilakone.core/missing-transition
                               :state  {:name :a}
                               :signal \c})))

(deftest apply-actions-test
  (let [value  1
        signal 2]
    (fact
      (apply-actions value
                     (fn [value signal action]
                       (+ value signal action))
                     signal
                     [3 4 5])
      => (-> (+ value signal 3)
             (+ signal 4)
             (+ signal 5)))))

(deftest apply-fsm-actions-test
  (let [value  1
        signal 2]
    (fact
      (apply-process-actions {:action! (fn [value signal action]
                                         (+ value signal action))
                              :value   value}
                             signal
                             [3 4 5])
      => {:value (-> (+ value signal 3)
                     (+ signal 4)
                     (+ signal 5))})))
