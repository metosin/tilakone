(ns tilakone.actions-and-guards-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :refer :all]))

(def count-ab
  [{:name        :start
    :transitions [{:on      \a
                   :to      :found-a
                   :actions [[:action :start :found-a]]}
                  {:on      _
                   :actions [[:action :start :start]]}]
    :enter       {:actions [[:enter :start]]}
    :leave       {:actions [[:leave :start]]}
    :stay        {:actions [[:stay :start]]}}
   {:name        :found-a
    :transitions [{:on      \a
                   :actions [[:action :found-a :found-a]]}
                  {:on      \b
                   :actions [[:action :found-a :found-a :via-guard-1]]}
                  {:on      _
                   :to      :start
                   :actions [[:action :found-a :start :via-b-_]]}]
    :enter       {:actions [[:enter :found-a]]}
    :leave       {:actions [[:leave :found-a]]}
    :stay        {:actions [[:stay :found-a]]}}])


(def count-ab-process {:states  count-ab
                       :action! (fn [{:keys [process signal action]}]
                                  (doto (-> process :value)
                                    (swap! conj (into [signal] action))))
                       :state   :start})


(deftest actions-test
  (let [count-ab (assoc count-ab-process :value (atom []))]
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
          [\x :enter :start]])))
