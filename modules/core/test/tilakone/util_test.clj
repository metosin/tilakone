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


(deftest get-state-test
  (fact
    (tu/get-process-current-state {:states [{:name :foo}
                                            {:name :bar}
                                            {:name :boz}]
                                   :state  :bar})
    => {:name :bar}))


;;
;; Guards:
;;

(let [ctx {:process {:value  1
                     :guard? (fn [ctx]
                               (case (-> ctx :guard)
                                 1 true
                                 2 false
                                 3 (throw (ex-info "fail" {:rejected :reason}))))}
           :signal  :some-signal}]
  (-> ctx
      (tu/apply-guards [1 2 3])
      :tilakone.util/guards-errors))

(deftest apply-guards!-test
  (let [ctx {:process {:value  1
                       :guard? (fn [ctx]
                                 (case (-> ctx :guard)
                                   1 true
                                   2 false
                                   3 (throw (ex-info "fail" {:rejected :reason}))))}
             :signal  :some-signal}]

    (fact
      (-> ctx
          (tu/apply-guards [1 1 1])
          (tu/apply-guards [1 1 1])
          :tilakone.util/guards-errors)
      => nil)

    (fact
      (-> ctx
          (tu/apply-guards [1 2 3])
          :tilakone.util/guards-errors)
      => [{:guard 3}
          {:guard 2}])

    (fact
      (-> ctx
          (tu/apply-guards [1 2 3])
          (tu/apply-guards [1 2 3])
          :tilakone.util/guards-errors)
      => [{:guard 3, :result (throws-ex-info "fail" {:rejected :reason})}
          {:guard 2, :result false}
          {:guard 3, :result (throws-ex-info "fail" {:rejected :reason})}
          {:guard 2, :result false}])))


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

#_(deftest apply-process-actions-test
    (let [ctx {:value  1
               :signal 2}]
      (fact
        (tu/apply-actions {:action! (fn [ctx]
                                      (+ (-> ctx :process :value)
                                         (-> ctx :signal)
                                         (-> ctx :action)))
                           :value   value}
                          signal
                          [3 4 5])
        => {:value (-> (+ value signal 3)
                       (+ signal 4)
                       (+ signal 5))})))
