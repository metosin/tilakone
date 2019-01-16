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
    (u/get-process-state {:states [{:name :foo}
                                   {:name :bar}
                                   {:name :boz}]}
                         :bar)
    => {:name :bar}))


(deftest get-state-test
  (fact
    (u/get-process-current-state {:states [{:name :foo}
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
  (let [visits (atom [])
        ctx    {:process {:states states
                          :state  :a
                          :guard? (fn [ctx]
                                    (swap! visits conj (-> ctx :guard))
                                    false)}}]

    (fact "stay in :a"
      (u/apply-guards ctx {:to :a, :guards [:a->a]})
      => [{:guard :a->a, :result false}
          {:guard :a, :result false}])

    (fact "to :b"
      (u/apply-guards ctx {:to :b, :guards [:a->b]})
      => [{:guard :a->}
          {:guard :a->b}
          {:guard :->b}])))


;;
;; Transitions:
;;


(def get-transition-guards #'u/get-transition-guards)


(deftest get-transition-guards-test
  (let [ctx {:process {:states states
                       :state  :a}}]
    (fact (get-transition-guards ctx {:to :a, :guards [:a->a]}) => [:a->a :a])
    (fact (get-transition-guards ctx {:to :b, :guards [:a->b]}) => [:a-> :a->b :->b])
    (fact (get-transition-guards ctx {:to :c, :guards [:a->c]}) => [:a-> :a->c :->c])))


(deftest get-transition-test
  (let [ctx        {:process {:states states
                              :state  :a}
                    :signal  \a}
        with-allow (fn [ctx allow]
                     (update ctx :process assoc :guard? (fn [ctx] (-> ctx :guard allow))))]

    (testing "with signal \\a, possible transitions are to :a and to :c"
      (fact (u/get-transitions ctx) => [{:to :a}, {:to :c}]))

    (testing "allow stay in a (:a->a and :a) and to c (a->, a->c and ->c), first is to :a"
      (let [ctx (with-allow ctx #{:a->a :a :a-> :a->c :->c})]
        (fact (u/get-transition ctx) => {:to :a})))

    (testing "disallowing just :a->a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow ctx #{:a :a-> :a->c :->c})]
        (fact (u/get-transition ctx) => {:to :c})))

    (testing "disallowing just :a (stay in :a) causes selection to be :c"
      (let [ctx (with-allow ctx #{:a->a :a-> :a->c :->c})]
        (fact (u/get-transition ctx) => {:to :c})))

    (testing "disallowing also :->c (enter :c) causes none to be available"
      (let [ctx (with-allow ctx #{:a->a :a-> :a->c})]
        (fact (u/get-transition ctx) =throws=> (throws-ex-info "transition from state [:a] with signal [\\a] forbidden by guard(s)"))))

    (testing "in state :b signal \\x is not allowed"
      (let [ctx (-> ctx
                    (update :process assoc :state :b)
                    (assoc :signal \x))]
        (fact (u/get-transition ctx) =throws=> (throws-ex-info "missing transition from state [:b] with signal [\\x]"))))))

;;
;; Actions:
;;


(deftest apply-actions-test
  (let [ctx {:process {:states  states
                       :state   :a
                       :action! (fn [{:keys [action] :as ctx}]
                                  (conj (-> ctx :process :value) action))
                       :value   []}
             :signal  \a}]
    (fact
      (u/apply-actions ctx {:to :a, :actions [:a->a]})
      => {:process {:value [:a->a :a]}})

    (fact
      (u/apply-actions ctx {:to :b, :actions [:a->b]})
      => {:process {:value [:a-> :a->b :->b]}})))
