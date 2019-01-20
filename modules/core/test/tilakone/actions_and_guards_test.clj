(ns tilakone.actions-and-guards-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :as tk :refer [_]]))


(def count-ab
  [{::tk/name        :start
    ::tk/transitions [{::tk/on      \a
                       ::tk/to      :found-a
                       ::tk/actions [[:action :start :found-a]]}
                      {::tk/on      _
                       ::tk/actions [[:action :start :start]]}]
    ::tk/enter       {::tk/actions [[:enter :start]]}
    ::tk/leave       {::tk/actions [[:leave :start]]}
    ::tk/stay        {::tk/actions [[:stay :start]]}}

   {::tk/name        :found-a
    ::tk/transitions [{::tk/on      \a
                       ::tk/actions [[:action :found-a :found-a]]}
                      {::tk/on      \b
                       ::tk/actions [[:action :found-a :found-a :via-guard-1]]}
                      {::tk/on      _
                       ::tk/to      :start
                       ::tk/actions [[:action :found-a :start :via-b-_]]}]
    ::tk/enter       {::tk/actions [[:enter :found-a]]}
    ::tk/leave       {::tk/actions [[:leave :found-a]]}
    ::tk/stay        {::tk/actions [[:stay :found-a]]}}])


(def count-ab-process {::tk/states  count-ab
                       ::tk/action! (fn [{::tk/keys [signal action] :as fsm}]
                                      (update fsm :trace conj (into [signal] action)))
                       ::tk/state   :start})


(deftest actions-test
  (let [count-ab (assoc count-ab-process :trace [])]
    (fact
      (-> (reduce tk/apply-signal count-ab "xxababbax")
          :trace)
      => [[\x :action :start :start]
          [\x :stay :start]
          [\x :action :start :start]
          [\x :stay :start]
          [\a :leave :start]
          [\a :action :start :found-a]
          [\a :enter :found-a]
          [\b :action :found-a :found-a :via-guard-1]
          [\b :stay :found-a]
          [\a :action :found-a :found-a]
          [\a :stay :found-a]
          [\b :action :found-a :found-a :via-guard-1]
          [\b :stay :found-a]
          [\b :action :found-a :found-a :via-guard-1]
          [\b :stay :found-a]
          [\a :action :found-a :found-a]
          [\a :stay :found-a]
          [\x :leave :found-a]
          [\x :action :found-a :start :via-b-_]
          [\x :enter :start]])))
