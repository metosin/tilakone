(ns tilakone.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as tk :refer [_]]))


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



(deftest apply-signal-test
  (let [process {:states  states
                 :state   :a
                 :guard?  (constantly true)
                 :action! (fn [{:keys [action] :as ctx}]
                            (conj (-> ctx :process :value) action))
                 :value   []}]
    (fact
      (tk/apply-signal process \a)
      => {:state :a
          :value [:a->a :a]})

    (fact
      (tk/apply-signal process \b)
      => {:state :b
          :value [:a-> :a->b :->b]})

    (fact
      (tk/apply-signal process \x)
      => {:state :c
          :value [:a-> :a->c :->c]}))

  (let [process {:states  states
                 :state   :b
                 :guard?  (constantly true)
                 :action! (fn [{:keys [action] :as ctx}]
                            (conj (-> ctx :process :value) action))
                 :value   []}]
    (fact
      (tk/apply-signal process \a)
      => {:state :a
          :value [:b-> :b->a :->a]})

    (fact
      (tk/apply-signal process \b)
      => {:state :b
          :value [:b->b :b]})

    (fact
      (tk/apply-signal process \x)
      =throws=> (throws-ex-info "missing transition from state [:b] with signal [\\x]"))))


(deftest apply-guards-test
  (let [process    {:states states
                    :state  :a}
        with-allow (fn [allow]
                     (assoc process :guard? (fn [ctx] (-> ctx :guard allow some?))))]
    (fact "don't allow anything, report all transitions with all guards returning `false`"
      (tk/apply-guards (with-allow #{}) \a)
      => [[{:to :a} [{:guard :a->a, :result false}
                     {:guard :a, :result false}]]
          [{:to :c} [{:guard :a->, :result false}
                     {:guard :a->c, :result false}
                     {:guard :->c, :result false}]]])

    (fact "allow :a->a"
      (tk/apply-guards (with-allow #{:a->a}) \a)
      => [[{:to :a} [{:guard :a, :result false}]]
          [{:to :c} [{:guard :a->, :result false}
                     {:guard :a->c, :result false}
                     {:guard :->c, :result false}]]])

    (fact "allow :a->a and :a"
      (tk/apply-guards (with-allow #{:a->a :a}) \a)
      => [[{:to :a} nil]
          [{:to :c} [{:guard :a->, :result false}
                     {:guard :a->c, :result false}
                     {:guard :->c, :result false}]]])

    (fact "allow :a->a, :a, :a->, :a->c and :->c"
      (tk/apply-guards (with-allow #{:a->a :a :a-> :a->c :->c}) \a)
      => [[{:to :a} nil]
          [{:to :c} nil]])))


(deftest transfers-to-test
  (let [process    {:states states
                    :state  :a}
        with-allow (fn [allow]
                     (assoc process :guard? (fn [ctx] (-> ctx :guard allow some?))))]
    (fact "don't allow anything, can't transfer anywhere"
      (tk/transfers-to (with-allow #{}) \a)
      => nil)

    (fact "allow :a->a and :a"
      (tk/transfers-to (with-allow #{:a->a :a}) \a)
      => :a)

    (fact "allow :a->, :a->c and :->c"
      (tk/transfers-to (with-allow #{:a-> :a->c :->c}) \a)
      => :c)

    (fact "allow :a->a, :a, :a->, :a->c and :->c"
      (tk/transfers-to (with-allow #{:a->a :a :a-> :a->c :->c}) \a)
      => :a)))
