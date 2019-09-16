(ns tilakone.util-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.util :as u]))


; can't require tilakone.core, that would be circular dependency:
(def _ :_)


;;
;; Generic utils:
;;


(def find-first #'u/find-first)


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
    (u/state {:states [{:name :foo}
                       {:name :bar}
                       {:name :boz}]}
             :bar)
    => {:name :bar}))


(deftest get-state-test
  (fact
    (u/current-state {:states [{:name :foo}
                               {:name :bar}
                               {:name :boz}]
                      :state  :bar})
    => {:name :bar}))


;;
;; Test process:
;;


(def states [{:name        :a
              :enter       {:guards [:->a], :actions [:->a]}
              :stay        {:guards [:a], :actions [:a]}
              :leave       {:guards [:a->], :actions [:a->]}
              :transitions [{:on \a, :to :a, :guards [:a->a], :actions [:a->a]}
                            {:on \b, :to :b, :guards [:a->b], :actions [:a->b]}
                            {:on _,, :to :c, :guards [:a->c], :actions [:a->c]}]}
             {:name        :b
              :enter       {:guards [:->b], :actions [:->b]}
              :stay        {:guards [:b], :actions [:b]}
              :leave       {:guards [:b->], :actions [:b->]}
              :transitions [{:on \a, :to :a, :guards [:b->a], :actions [:b->a]}
                            {:on \b, :to :b, :guards [:b->b], :actions [:b->b]}]}
             {:name  :c
              :enter {:guards [:->c], :actions [:->c]}}])


;;
;; Guards:
;;


(deftest apply-guards!-test
  (let [fsm {:states states
             :state  :a
             :guard? (fn [fsm signal guard]
                       false)}]

    (fact "stay in :a"
      (u/apply-guards fsm :signal {:to :a, :guards [:a->a]})
      => [{:guard :a->a, :result false}
          {:guard :a, :result false}])

    (fact "to :b"
      (u/apply-guards fsm :signal {:to :b, :guards [:a->b]})
      => [{:guard :a->}
          {:guard :a->b}
          {:guard :->b}])))


;;
;; Transitions:
;;


(def get-transition-guards #'u/get-transition-guards)


(deftest get-transition-guards-test
  (let [fsm {:states states
             :state  :a}]
    (fact (get-transition-guards fsm {:to :a, :guards [:a->a]}) => [:a->a :a])
    (fact (get-transition-guards fsm {:to :b, :guards [:a->b]}) => [:a-> :a->b :->b])
    (fact (get-transition-guards fsm {:to :c, :guards [:a->c]}) => [:a-> :a->c :->c])))


(deftest get-transition-test
  (let [fsm        {:states states
                    :state  :a}
        with-allow (fn [ctx allow]
                     (assoc ctx :guard?
                                (fn [_ _ guard]
                                  (allow guard))))]

    (testing "with signal \\a, possible transitions are to :a and to :c"
      (fact (u/get-transitions fsm \a) => [{:to :a}, {:to :c}]))

    (testing "allow stay in a (:a->a and :a) and to c (a->, a->c and ->c), first is to :a"
      (let [ctx (with-allow fsm #{:a->a :a :a-> :a->c :->c})]
        (fact (u/get-transition ctx \a) => {:to :a})))

    (testing "disallowing just :a->a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow fsm #{:a :a-> :a->c :->c})]
        (fact (u/get-transition ctx \a) => {:to :c})))

    (testing "disallowing just :a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow fsm #{:a->a :a-> :a->c :->c})]
        (fact (u/get-transition ctx \a) => {:to :c})))

    (testing "disallowing also :->c (enter :c) causes none to be available"
      (let [ctx (with-allow fsm #{:a->a :a-> :a->c})]
        (fact (u/get-transition ctx \a) =throws=> (throws-ex-info "transition from state [:a] with signal [\\a] forbidden by guard(s)"))))

    (testing "in state :b signal \\x is not allowed"
      (let [ctx (-> fsm
                    (assoc :state :b)
                    (assoc :signal \x))]
        (fact (u/get-transition ctx \x) =throws=> (throws-ex-info "missing transition from state [:b] with signal [\\x]"))))))

;;
;; Actions:
;;


(deftest apply-actions-test
  (let [fsm {:states  states
             :state   :a
             :action! (fn [fsm signal action]
                        (update fsm :trace conj [signal action]))
             :trace   []}]
    (fact
      (u/apply-actions fsm :signal {:to :a, :actions [:a->a]})
      => {:trace [[:signal :a->a]
                  [:signal :a]]})

    (fact
      (u/apply-actions fsm :signal {:to :b, :actions [:a->b]})
      => {:trace [[:signal :a->]
                  [:signal :a->b]
                  [:signal :->b]]})))
