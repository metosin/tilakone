(ns tilakone.schema-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as tk :refer [_]]
            [tilakone.schema :as s]))


;;
;; Tests:
;;


(deftest validate-states-test
  (fact "valid states data"
    (s/validate-states [{:name        :a
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
