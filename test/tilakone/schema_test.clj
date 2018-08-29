(ns tilakone.schema-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as fsm :refer [_]]
            [tilakone.schema :as s]))

(def count-ab-states {:start   {:transitions {\a {:to :found-a}}}
                      :found-a {:transitions {\a {:to :found-a}
                                              \b [[:max?] {:to :start}
                                                  _ {:to :found-a}]
                                              _  {:to      :start
                                                  :actions [[:inc-val]]}}}})

;;
;; Tests:
;;


(s/validate-states count-ab-states)

(deftest validate-states-test
  (fact "valid states data"
    (s/validate-states count-ab-states) => truthy)
  (fact "unknown target states"
    (s/validate-states {:a {:transitions {1 {:to :a}}}
                        :b {:transitions {2 {:to :FAIL-X}
                                          3 {:to :FAIL-Y}}}
                        :d {:transitions {4 {:to :FAIL-Z}
                                          5 {:to :a}}}})
    => (throws-ex-info "unknown target states"
                       {:errors (in-any-order [[:b 2 :FAIL-X]
                                               [:b 3 :FAIL-Y]
                                               [:d 4 :FAIL-Z]])})))
