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
    (simple-transition? {:to :foo}) => truthy)
  (fact
    (simple-transition? [1 2]) => falsey))

(deftest guard-matcher-test
  (fact
    (-> {:value 1 :guard? =}
        (guard-matcher 1)
        (apply [[[1] :v1]]))
    => :v1)

  (fact
    (-> {:value 1 :guard? =}
        (guard-matcher 2)
        (apply [[[1] :v1]]))
    => nil)

  (fact
    (-> {:value 1 :guard? =}
        (guard-matcher 2)
        (apply [[_ :v_]]))
    => :v_))

(def test-fsm
  {:states {:simple  {:transitions {\a {:to :next}}}
            :default {:transitions {\a {:to :match-a}
                                    _  {:to :match-_}}}
            :guarded {:transitions {\a [[1] {:to :ga-1-state}
                                        [2] {:to :ga-2-state}
                                        _ {:to :ga-_-state}]}}}
   :guard? (fn [type value signal] (= type value))})

(deftest get-transition-test
  (fact "simple transition"
    (get-transition test-fsm
                    (-> test-fsm :states :simple)
                    \a)
    => {:to :next})

  (fact "default signal: a"
    (get-transition test-fsm
                    (-> test-fsm :states :default)
                    \a)
    => {:to :match-a})

  (fact "default signal: _"
    (get-transition test-fsm
                    (-> test-fsm :states :default)
                    \x)
    => {:to :match-_})

  (fact "guarded transition: value = 1"
    (get-transition (-> test-fsm (assoc :value 1))
                    (-> test-fsm :states :guarded)
                    \a)
    => {:to :ga-1-state})

  (fact "guarded transition: value = 2"
    (get-transition (-> test-fsm (assoc :value 2))
                    (-> test-fsm :states :guarded)
                    \a)
    => {:to :ga-2-state})

  (fact "guarded transition: value = 3"
    (get-transition (-> test-fsm (assoc :value 3))
                    (-> test-fsm :states :guarded)
                    \a)
    => {:to :ga-_-state}))

(deftest apply-actions-test
  (let [value  1
        signal 2
        value  (+ value signal 2 3 4)
        value  (* value signal 2)
        value  (- value signal 4)]
    (fact
      value => 42)
    (fact
      (apply-actions {:value   1
                      :action! (fn [f v s & args]
                                 (apply f v s args))}
                     2
                     [[+ 2 3 4]
                      [* 2]
                      [- 4]])
      => {:value 42})))
