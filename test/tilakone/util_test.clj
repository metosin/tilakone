(ns tilakone.util-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.util :refer :all]))

; can't require tilakone.core, that would be circular dependency:
(def _ :tilakone.core/_)

(deftest message->str-test
  (fact
    (message->str "foo") => "foo")
  (fact
    (message->str ["foo" :bar 42]) => "foo :bar 42"))

(deftest error!-test
  (fact
    (error! ["foo" :bar] {:foo :bar})
    => (throws-ex-info "foo :bar" {:type :tilakone.core/error
                                   :foo  :bar})))

(deftest simple-transition?-test
  (fact
    (simple-transition? {:state :foo}) => truthy)
  (fact
    (simple-transition? [1 2]) => falsey))

(deftest guard-matcher-test
  (fact
    (-> {:value 1 :guard-fn =}
        (guard-matcher)
        (apply [[[1] :v1]]))
    => :v1)

  (fact
    (-> {:value 2 :guard-fn =}
        (guard-matcher)
        (apply [[[1] :v1]]))
    => nil)

  (fact
    (-> {:value 2 :guard-fn =}
        (guard-matcher)
        (apply [[_ :v_]]))
    => :v_))

(def get-transition-test-fsm
  {:states   {:simple  {:transitions {\a {:state :next}}}
              :default {:transitions {\a {:state :match-a}
                                      _  {:state :match-_}}}
              :guarded {:transitions {\a [[[1] {:state :ga-1-state}]
                                          [[2] {:state :ga-2-state}]
                                          [_ {:state :ga-_-state}]]}}}
   :guard-fn =})

(deftest get-transition-test
  (fact "simple transition"
    (get-transition get-transition-test-fsm
                    (-> get-transition-test-fsm :states :simple)
                    \a)
    => {:state :next})

  (fact "default signal: a"
    (get-transition get-transition-test-fsm
                    (-> get-transition-test-fsm :states :default)
                    \a)
    => {:state :match-a})

  (fact "default signal: _"
    (get-transition get-transition-test-fsm
                    (-> get-transition-test-fsm :states :default)
                    \x)
    => {:state :match-_})

  (fact "guarded transition: value = 1"
    (get-transition (-> get-transition-test-fsm (assoc :value 1))
                    (-> get-transition-test-fsm :states :guarded)
                    \a)
    => {:state :ga-1-state})

  (fact "guarded transition: value = 2"
    (get-transition (-> get-transition-test-fsm (assoc :value 2))
                    (-> get-transition-test-fsm :states :guarded)
                    \a)
    => {:state :ga-2-state})

  (fact "guarded transition: value = 3"
    (get-transition (-> get-transition-test-fsm (assoc :value 3))
                    (-> get-transition-test-fsm :states :guarded)
                    \a)
    => {:state :ga-_-state}))

(deftest apply-actions-test
  (fact
    (apply-actions {:value 1
                    :action-fn (fn [f v & args]
                                 (apply f v args))}
                   [[+ 2 3 4]
                    [* 5]
                    [- 8]])
    => {:value 42}))
