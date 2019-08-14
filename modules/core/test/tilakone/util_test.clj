(ns tilakone.util-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as tk :refer [_]]
            [tilakone.util :as tku]))


;;
;; Generic utils:
;;


(def find-first #'tku/find-first)


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
    (tku/state {::tk/states [{::tk/name :foo}
                             {::tk/name :bar}
                             {::tk/name :boz}]}
               :bar)
    => {::tk/name :bar}))


(deftest get-state-test
  (fact
    (tku/current-state {::tk/states [{::tk/name :foo}
                                     {::tk/name :bar}
                                     {::tk/name :boz}]
                        ::tk/state  :bar})
    => {::tk/name :bar}))


;;
;; Test process:
;;


(def states [{::tk/name        :a
              ::tk/enter       {::tk/guards [:->a], ::tk/actions [:->a]}
              ::tk/stay        {::tk/guards [:a], ::tk/actions [:a]}
              ::tk/leave       {::tk/guards [:a->], ::tk/actions [:a->]}
              ::tk/transitions [{::tk/on \a, ::tk/to :a, ::tk/guards [:a->a], ::tk/actions [:a->a]}
                                {::tk/on \b, ::tk/to :b, ::tk/guards [:a->b], ::tk/actions [:a->b]}
                                {::tk/on _,, ::tk/to :c, ::tk/guards [:a->c], ::tk/actions [:a->c]}]}
             {::tk/name        :b
              ::tk/enter       {::tk/guards [:->b], ::tk/actions [:->b]}
              ::tk/stay        {::tk/guards [:b], ::tk/actions [:b]}
              ::tk/leave       {::tk/guards [:b->], ::tk/actions [:b->]}
              ::tk/transitions [{::tk/on \a, ::tk/to :a, ::tk/guards [:b->a], ::tk/actions [:b->a]}
                                {::tk/on \b, ::tk/to :b, ::tk/guards [:b->b], ::tk/actions [:b->b]}]}
             {::tk/name  :c
              ::tk/enter {::tk/guards [:->c], ::tk/actions [:->c]}}])


;;
;; Guards:
;;


(deftest apply-guards!-test
  (let [fsm {::tk/states states
             ::tk/state  :a
             ::tk/guard? (fn [_ _]
                           false)}]

    (fact "stay in :a"
      (tku/apply-guards fsm :signal {::tk/to :a, ::tk/guards [:a->a]})
      => [{::tk/guard :a->a, ::tk/result false}
          {::tk/guard :a, ::tk/result false}])

    (fact "to :b"
      (tku/apply-guards fsm :signal {::tk/to :b, ::tk/guards [:a->b]})
      => [{::tk/guard :a->}
          {::tk/guard :a->b}
          {::tk/guard :->b}])))


;;
;; Transitions:
;;


(def get-transition-guards #'tku/get-transition-guards)


(deftest get-transition-guards-test
  (let [fsm {::tk/states states
             ::tk/state  :a}]
    (fact (get-transition-guards fsm {::tk/to :a, ::tk/guards [:a->a]}) => [:a->a :a])
    (fact (get-transition-guards fsm {::tk/to :b, ::tk/guards [:a->b]}) => [:a-> :a->b :->b])
    (fact (get-transition-guards fsm {::tk/to :c, ::tk/guards [:a->c]}) => [:a-> :a->c :->c])))


(deftest get-transition-test
  (let [fsm        {::tk/states states
                    ::tk/state  :a}
        with-allow (fn [ctx allow]
                     (assoc ctx ::tk/guard?
                                (fn [{::tk/keys [guard]} _]
                                  (allow guard))))]

    (testing "with signal \\a, possible transitions are to :a and to :c"
      (fact (tku/get-transitions fsm \a) => [{::tk/to :a}, {::tk/to :c}]))

    (testing "allow stay in a (:a->a and :a) and to c (a->, a->c and ->c), first is to :a"
      (let [ctx (with-allow fsm #{:a->a :a :a-> :a->c :->c})]
        (fact (tku/get-transition ctx \a) => {::tk/to :a})))

    (testing "disallowing just :a->a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow fsm #{:a :a-> :a->c :->c})]
        (fact (tku/get-transition ctx \a) => {::tk/to :c})))

    (testing "disallowing just :a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow fsm #{:a->a :a-> :a->c :->c})]
        (fact (tku/get-transition ctx \a) => {::tk/to :c})))

    (testing "disallowing also :->c (enter :c) causes none to be available"
      (let [ctx (with-allow fsm #{:a->a :a-> :a->c})]
        (fact (tku/get-transition ctx \a) =throws=> (throws-ex-info "transition from state [:a] with signal [\\a] forbidden by guard(s)"))))

    (testing "in state :b signal \\x is not allowed"
      (let [ctx (-> fsm
                    (assoc ::tk/state :b)
                    (assoc ::tk/signal \x))]
        (fact (tku/get-transition ctx \x) =throws=> (throws-ex-info "missing transition from state [:b] with signal [\\x]"))))))

;;
;; Actions:
;;


(deftest apply-actions-test
  (let [fsm {::tk/states  states
             ::tk/state   :a
             ::tk/action! (fn [{::tk/keys [signal action] :as fsm} value]
                            (conj value [signal action]))
             ::tk/value   []}]
    (fact
      (tku/apply-actions (assoc fsm ::tk/signal :dignal
                                    ::from-state from-state
                                    ::transition transition
                                    ::to-state   to-state
                                    {::tk/to :a, ::tk/actions [:a->a]}))
      => {::tk/value [[:signal :a->a]
                      [:signal :a]]})

    (fact
      (tku/apply-actions fsm :signal {::tk/to :b, ::tk/actions [:a->b]})
      => {::tk/value [[:signal :a->]
                      [:signal :a->b]
                      [:signal :->b]]})))

