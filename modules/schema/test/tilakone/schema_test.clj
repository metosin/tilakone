(ns tilakone.schema-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as fsm :refer [_]]
            [tilakone.schema :as s]))

;;
;; Tests:
;;


(deftest validate-states-test
  (fact "valid states data"
    (s/validate-states [{:name        :start
                         :transitions [{:to :found-a
                                        :on \a}]}
                        {:name        :found-a
                         :transitions [{:to :found-a}]}])
    => truthy)

  (fact "unknown target states"
    (s/validate-states [{:name        :start
                         :transitions [{:to :found-a
                                        :on \a}]}
                        {:name        :found-a
                         :transitions [{:to :found-x}]}])
    =throws=> (throws-ex-info "unknown target states: state [:found-a] has transition [anonymous] to unknown state [:found-x]"
                              {:type   :tilakone.core/error
                               :errors [{:state      {:name :found-a}
                                         :transition {:to :found-x}}]})))
