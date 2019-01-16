(ns tilakone.util-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.util :as tu]))


; can't require tilakone.core, that would be circular dependency:
(def _ :tilakone.core/_)


;;
;; Generic utils:
;;


(def find-first #'tu/find-first)


(deftest find-first-test
  (fact
    (find-first some? [nil 42 nil]) => 42)
  (fact
    (find-first some? [nil nil]) => nil))


;;
;; State:
;;


(deftest get-state-test
  (fact
    (tu/get-process-state {:states [{:name :foo}
                                    {:name :bar}
                                    {:name :boz}]}
                          :bar)
    => {:name :bar}))


;;
;; Guards:
;;


(deftest apply-guards!-test
  (let [transition {:guards [1 2 3]}]
    (fact
      (tu/apply-guards! transition
                        {:process {:value  1
                                   :guard? (constantly true)}}
                        {})
      => truthy)
    (fact
      (tu/apply-guards! transition
                        {:process {:value  1
                                   :guard? (fn [ctx]
                                             (case (-> ctx :guard)
                                               1 true
                                               2 false
                                               3 (throw (ex-info "fail" {:rejected :reason}))))}
                         :signal  :some-signal}
                        {:name :state-x})
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
                                                  :message "fail"}]}))))


;;
;; Transitions:
;;


(def process {:states [{:name        :foo
                        :transitions [{:on \a, :to :a}
                                      {:on \b, :to :b}
                                      {:on _,, :to :c}]}
                       {:name        :a
                        :transitions [{:on \a, :to :a}
                                      {:on \b, :to :b}]}]})


(def find-transition #'tu/find-transition)


(deftest find-transition-test
  (let [state       (tu/get-process-state process :foo)
        transitions (:transitions state)
        ctx         {:process process}]
    (fact
      (find-transition (assoc ctx :signal \a) transitions)
      => {:to :a})
    (fact
      (find-transition (assoc ctx :signal \b) transitions)
      => {:to :b})
    (fact
      (find-transition (assoc ctx :signal \x) transitions)
      => {:to :c})))


(deftest get-transition-test
  (fact
    (tu/get-transition process (tu/get-process-state process :a) \a)
    => {:to :a})
  (fact
    (tu/get-transition process (tu/get-process-state process :a) \b)
    => {:to :b})
  (fact
    (tu/get-transition process (tu/get-process-state process :a) \c)
    =throws=> (throws-ex-info "missing transition from state [:a] with signal [\\c]"
                              {:type   :tilakone.core/error
                               :error  :tilakone.core/missing-transition
                               :state  {:name :a}
                               :signal \c})))


;;
;; Actions:
;;


(deftest apply-process-actions-test
  (let [value  1
        signal 2]
    (fact
      (tu/apply-process-actions {:action! (fn [ctx]
                                            (+ (-> ctx :process :value)
                                               (-> ctx :signal)
                                               (-> ctx :action)))
                                 :value   value}
                                signal
                                [3 4 5])
      => {:value (-> (+ value signal 3)
                     (+ signal 4)
                     (+ signal 5))})))
