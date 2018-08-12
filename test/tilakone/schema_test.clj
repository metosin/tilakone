(ns tilakone.schema-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as fsm :refer [_]]
            [tilakone.schema :as s]))

; Example state machine from https://github.com/cdorrat/reduce-fsm#basic-fsm:

(def count-ab-states {:start   {:transitions {\a {:state :found-a}}}
                      :found-a {:transitions {\a {:state :found-a}
                                              \b {:state   :start
                                                  :actions [[:inc-val]]}
                                              _  {:state :start}}}})

;;
;; Tests:
;;


(deftest validate-states-test
  (fact "valid states data"
        (s/validate-states count-ab-states) => truthy)
  (fact "unknown target states"
        (s/validate-states {:a {:transitions {1 {:state :a}}}
                            :b {:transitions {2 {:state :FAIL-X}
                                              3 {:state :FAIL-Y}}}
                            :d {:transitions {4 {:state :FAIL-Z}
                                              5 {:state :a}}}})
        => (throws-ex-info "unknown target states"
                           {:errors (in-any-order [[:b 2 :FAIL-X]
                                                   [:b 3 :FAIL-Y]
                                                   [:d 4 :FAIL-Z]])})))
