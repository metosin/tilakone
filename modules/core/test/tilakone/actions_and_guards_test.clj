(ns tilakone.actions-and-guards-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :refer :all]))

(def count-ab-states
  [{:name        :start
    :transitions [{:on      \a
                   :to      :found-a
                   :actions [[:action :start :found-a]]}
                  {:on      _
                   :to      :start
                   :actions [[:action :start :start]]}]
    :enter       [[:enter :start]]
    :leave       [[:leave :start]]
    :stay        [[:stay :start]]}
   {:name        :found-a
    :transitions [{:on      \a
                   :to      :found-a
                   :actions [[:action :found-a :found-a]]}
                  {:on      \b
                   :to      :found-a
                   :actions [[:action :found-a :found-a :via-guard-1]]}
                  {:on      _
                   :to      :start
                   :actions [[:action :found-a :start :via-b-_]]}]
    :enter       [[:enter :found-a]]
    :leave       [[:leave :found-a]]
    :stay        [[:stay :found-a]]}])

(def count-ab {:states  count-ab-states
               :action! (fn [value signal action]
                          (swap! value conj (into [signal] action))
                          value)
               :state   :start
               :value   (atom [])})

(deftest actions-test
  (fact
    (-> (reduce apply-signal count-ab "xxababbax")
        :value
        deref)
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
        [\x :enter :start]]))
