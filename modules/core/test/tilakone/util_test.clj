(ns tilakone.util-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.util :as u]))


; can't require tilakone.core, that would be circular dependency:
(def _ :tilakone.core/_)


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
    (u/get-process-state {:tilakone.core/states [{:tilakone.core/name :foo}
                                                 {:tilakone.core/name :bar}
                                                 {:tilakone.core/name :boz}]}
                         :bar)
    => {:tilakone.core/name :bar}))


(deftest get-state-test
  (fact
    (u/get-process-current-state {:tilakone.core/states [{:tilakone.core/name :foo}
                                                         {:tilakone.core/name :bar}
                                                         {:tilakone.core/name :boz}]
                                  :tilakone.core/state  :bar})
    => {:tilakone.core/name :bar}))


;;
;; Test process:
;;


(def states [{:tilakone.core/name        :a
              :tilakone.core/enter       {:tilakone.core/guards [:->a], :tilakone.core/actions [:->a]}
              :tilakone.core/stay        {:tilakone.core/guards [:a], :tilakone.core/actions [:a]}
              :tilakone.core/leave       {:tilakone.core/guards [:a->], :tilakone.core/actions [:a->]}
              :tilakone.core/transitions [{:tilakone.core/on \a, :tilakone.core/to :a, :tilakone.core/guards [:a->a], :tilakone.core/actions [:a->a]}
                                          {:tilakone.core/on \b, :tilakone.core/to :b, :tilakone.core/guards [:a->b], :tilakone.core/actions [:a->b]}
                                          {:tilakone.core/on _,, :tilakone.core/to :c, :tilakone.core/guards [:a->c], :tilakone.core/actions [:a->c]}]}
             {:tilakone.core/name        :b
              :tilakone.core/enter       {:tilakone.core/guards [:->b], :tilakone.core/actions [:->b]}
              :tilakone.core/stay        {:tilakone.core/guards [:b], :tilakone.core/actions [:b]}
              :tilakone.core/leave       {:tilakone.core/guards [:b->], :tilakone.core/actions [:b->]}
              :tilakone.core/transitions [{:tilakone.core/on \a, :tilakone.core/to :a, :tilakone.core/guards [:b->a], :tilakone.core/actions [:b->a]}
                                          {:tilakone.core/on \b, :tilakone.core/to :b, :tilakone.core/guards [:b->b], :tilakone.core/actions [:b->b]}]}
             {:tilakone.core/name  :c
              :tilakone.core/enter {:tilakone.core/guards [:->c], :tilakone.core/actions [:->c]}}])


;;
;; Guards:
;;


(deftest apply-guards!-test
  (let [visits (atom [])
        ctx    {:tilakone.core/process {:tilakone.core/states states
                                        :tilakone.core/state  :a
                                        :tilakone.core/guard? (fn [ctx]
                                                                (swap! visits conj (-> ctx :tilakone.core/guard))
                                                                false)}}]

    (fact "stay in :a"
      (u/apply-guards ctx {:tilakone.core/to :a, :tilakone.core/guards [:a->a]})
      => [{:tilakone.core/guard :a->a, :tilakone.core/result false}
          {:tilakone.core/guard :a, :tilakone.core/result false}])

    (fact "to :b"
      (u/apply-guards ctx {:tilakone.core/to :b, :tilakone.core/guards [:a->b]})
      => [{:tilakone.core/guard :a->}
          {:tilakone.core/guard :a->b}
          {:tilakone.core/guard :->b}])))


;;
;; Transitions:
;;


(def get-transition-guards #'u/get-transition-guards)


(deftest get-transition-guards-test
  (let [ctx {:tilakone.core/process {:tilakone.core/states states
                                     :tilakone.core/state  :a}}]
    (fact (get-transition-guards ctx {:tilakone.core/to :a, :tilakone.core/guards [:a->a]}) => [:a->a :a])
    (fact (get-transition-guards ctx {:tilakone.core/to :b, :tilakone.core/guards [:a->b]}) => [:a-> :a->b :->b])
    (fact (get-transition-guards ctx {:tilakone.core/to :c, :tilakone.core/guards [:a->c]}) => [:a-> :a->c :->c])))


(deftest get-transition-test
  (let [ctx        {:tilakone.core/process {:tilakone.core/states states
                                            :tilakone.core/state  :a}
                    :tilakone.core/signal  \a}
        with-allow (fn [ctx allow]
                     (update ctx :tilakone.core/process assoc :tilakone.core/guard? (fn [ctx] (-> ctx :tilakone.core/guard allow))))]

    (testing "with signal \\a, possible transitions are to :a and to :c"
      (fact (u/get-transitions ctx) => [{:tilakone.core/to :a}, {:tilakone.core/to :c}]))

    (testing "allow stay in a (:a->a and :a) and to c (a->, a->c and ->c), first is to :a"
      (let [ctx (with-allow ctx #{:a->a :a :a-> :a->c :->c})]
        (fact (u/get-transition ctx) => {:tilakone.core/to :a})))

    (testing "disallowing just :a->a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow ctx #{:a :a-> :a->c :->c})]
        (fact (u/get-transition ctx) => {:tilakone.core/to :c})))

    (testing "disallowing just :a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow ctx #{:a->a :a-> :a->c :->c})]
        (fact (u/get-transition ctx) => {:tilakone.core/to :c})))

    (testing "disallowing also :->c (enter :c) causes none to be available"
      (let [ctx (with-allow ctx #{:a->a :a-> :a->c})]
        (fact (u/get-transition ctx) =throws=> (throws-ex-info "transition from state [:a] with signal [\\a] forbidden by guard(s)"))))

    (testing "in state :b signal \\x is not allowed"
      (let [ctx (-> ctx
                    (update :tilakone.core/process assoc :tilakone.core/state :b)
                    (assoc :tilakone.core/signal \x))]
        (fact (u/get-transition ctx) =throws=> (throws-ex-info "missing transition from state [:b] with signal [\\x]"))))))

;;
;; Actions:
;;


(deftest apply-actions-test
  (let [ctx {:tilakone.core/process {:tilakone.core/states  states
                                     :tilakone.core/state   :a
                                     :tilakone.core/action! (fn [{:tilakone.core/keys [action] :as ctx}]
                                                              (update-in ctx [:tilakone.core/process :trace] conj action))
                                     :trace                 []}
             :tilakone.core/signal  \a}]
    (fact
      (u/apply-actions ctx {:tilakone.core/to :a, :tilakone.core/actions [:a->a]})
      => {:tilakone.core/process {:trace [:a->a :a]}})

    (fact
      (u/apply-actions ctx {:tilakone.core/to :b, :tilakone.core/actions [:a->b]})
      => {:tilakone.core/process {:trace [:a-> :a->b :->b]}})))
