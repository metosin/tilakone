(ns tilakone.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as tk :refer [_]]))


(def states [{::tk/name        :a
              ::tk/enter       {::tk/guards  [:->a]
                                ::tk/actions [:->a]}
              ::tk/stay        {::tk/guards  [:a]
                                ::tk/actions [:a]}
              ::tk/leave       {::tk/guards  [:a->]
                                ::tk/actions [:a->]}
              ::tk/transitions [{::tk/on      \a
                                 ::tk/to      :a
                                 ::tk/guards  [:a->a]
                                 ::tk/actions [:a->a]}

                                {::tk/on      \b
                                 ::tk/to      :b
                                 ::tk/guards  [:a->b]
                                 ::tk/actions [:a->b]}

                                {::tk/on     _
                                 ::tk/to     :c
                                 ::tk/guards [:a->c], ::tk/actions [:a->c]}]}

             {::tk/name        :b
              ::tk/enter       {::tk/guards  [:->b]
                                ::tk/actions [:->b]}
              ::tk/stay        {::tk/guards  [:b]
                                ::tk/actions [:b]}
              ::tk/leave       {::tk/guards  [:b->]
                                ::tk/actions [:b->]}
              ::tk/transitions [{::tk/on      \a
                                 ::tk/to      :a
                                 ::tk/guards  [:b->a]
                                 ::tk/actions [:b->a]}

                                {::tk/on      \b
                                 ::tk/to      :b
                                 ::tk/guards  [:b->b]
                                 ::tk/actions [:b->b]}]}

             {::tk/name  :c
              ::tk/enter {::tk/guards  [:->c]
                          ::tk/actions [:->c]}}])



(deftest apply-signal-test
  (let [process {::tk/states  states
                 ::tk/state   :a
                 ::tk/guard?  (constantly true)
                 ::tk/action! (fn [{::tk/keys [action] :as ctx}]
                                (update ctx :trace conj action))
                 :trace       []}]
    (fact
      (tk/apply-signal process \a)
      => {::tk/state :a
          :trace     [:a->a :a]})

    (fact
      (tk/apply-signal process \b)
      => {::tk/state :b
          :trace     [:a-> :a->b :->b]})

    (fact
      (tk/apply-signal process \x)
      => {::tk/state :c
          :trace     [:a-> :a->c :->c]}))

  (let [process {::tk/states  states
                 ::tk/state   :b
                 ::tk/guard?  (constantly true)
                 ::tk/action! (fn [{::tk/keys [action] :as ctx}]
                                (update ctx :trace conj action))
                 :trace       []}]
    (fact
      (tk/apply-signal process \a)
      => {::tk/state :a
          :trace     [:b-> :b->a :->a]})

    (fact
      (tk/apply-signal process \b)
      => {::tk/state :b
          :trace     [:b->b :b]})

    (fact
      (tk/apply-signal process \x)
      =throws=> (throws-ex-info "missing transition from state [:b] with signal [\\x]"))))


(deftest apply-guards-test
  (let [process    {::tk/states states
                    ::tk/state  :a}
        with-allow (fn [allow]
                     (assoc process ::tk/guard? (fn [ctx] (-> ctx ::tk/guard allow some?))))]
    (fact "don't allow anything, report all transitions with all guards returning `false`"
      (tk/apply-guards (with-allow #{}) \a)
      => [[{:tilakone.core/to :a} [{::tk/guard :a->a, ::tk/result false}
                                   {::tk/guard :a, ::tk/result false}]]
          [{:tilakone.core/to :c} [{::tk/guard :a->, ::tk/result false}
                                   {::tk/guard :a->c, ::tk/result false}
                                   {::tk/guard :->c, ::tk/result false}]]])

    (fact "allow :a->a"
      (tk/apply-guards (with-allow #{:a->a}) \a)
      => [[{:tilakone.core/to :a} [{::tk/guard :a, ::tk/result false}]]
          [{:tilakone.core/to :c} [{::tk/guard :a->, ::tk/result false}
                                   {::tk/guard :a->c, ::tk/result false}
                                   {::tk/guard :->c, ::tk/result false}]]])

    (fact "allow :a->a and :a"
      (tk/apply-guards (with-allow #{:a->a :a}) \a)
      => [[{:tilakone.core/to :a} nil]
          [{:tilakone.core/to :c} [{::tk/guard :a->, ::tk/result false}
                                   {::tk/guard :a->c, ::tk/result false}
                                   {::tk/guard :->c, ::tk/result false}]]])

    (fact "allow :a->a, :a, :a->, :a->c and :->c"
      (tk/apply-guards (with-allow #{:a->a :a :a-> :a->c :->c}) \a)
      => [[{:tilakone.core/to :a} nil]
          [{:tilakone.core/to :c} nil]])))


(deftest transfers-to-test
  (let [process    {::tk/states states
                    ::tk/state  :a}
        with-allow (fn [allow]
                     (assoc process ::tk/guard? (fn [ctx] (-> ctx ::tk/guard allow some?))))]
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
