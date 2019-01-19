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
    (s/validate-states [{::tk/name        :a
                         ::tk/enter       {::tk/guards [:->a], ::tk/actions [:->a]}
                         ::tk/stay        {::tk/guards [:a], ::tk/actions [:a]}
                         ::tk/leave       {::tk/guards [:a->], ::tk/actions [:a->]}
                         ::tk/transitions [{::tk/on \a, ::tk/to :a, ::tk/guards [:a->a], ::tk/actions [:a->a]}
                                           {::tk/on \b, ::tk/to :b, ::tk/guards [:a->b], ::tk/actions [:a->b]}
                                           {::tk/on _,, ::tk/to :c, ::tk/guards [:a->c], ::tk/actions [:a->c]}]}

                        {::tk/name        :b
                         ::tk/enter       {::tk/guards [:->b], ::tk/actions [:->b]}
                         ::tk/stay        {::tk/guards [:b], ::tk/actions [:b]}
                         ::tk/leave       {::tk/guards [:b->], ::tk/actions [:b->]}
                         ::tk/transitions [{::tk/on \a, ::tk/to :a, ::tk/guards [:b->a], ::tk/actions [:b->a]}
                                           {::tk/on \b, ::tk/to :b, ::tk/guards [:b->b], ::tk/actions [:b->b]}]}

                        {::tk/name  :c
                         ::tk/enter {::tk/guards [:->c], ::tk/actions [:->c]}}])
    => truthy)

  (fact "unknown target states"
    (s/validate-states [{::tk/name        :start
                         ::tk/transitions [{::tk/to :found-a
                                            ::tk/on \a}]}
                        {::tk/name        :found-a
                         ::tk/transitions [{::tk/to :found-x}]}])
    =throws=> (throws-ex-info "unknown target states: state [:found-a] has transition [anonymous] to unknown state [:found-x]"
                              {:type   :tilakone.core/error
                               :errors [{::tk/state      {::tk/name :found-a}
                                         ::tk/transition {::tk/to :found-x}}]})))
