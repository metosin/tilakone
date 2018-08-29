(ns tilakone.actions-and-guards-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [tilakone.core :refer :all]))

(def count-ab-states
  {:start   {:transitions {\a {:to      :found-a
                               :actions [[:action :start :found-a]]}
                           _  {:to      :start
                               :actions [[:action :start :start]]}}
             :enter       [[:enter :start]]
             :leave       [[:leave :start]]
             :stay        [[:stay :start]]}

   :found-a {:transitions {\a {:to      :found-a
                               :actions [[:action :found-a :found-a]]}
                           \b [[:guard false :found-a \b] {:to      :found-a
                                                           :actions [[:action :found-a :found-a :via-guard-1]]}
                               [:guard true :found-a \b] {:to      :found-a
                                                          :actions [[:action :found-a :found-a :via-guard-2]]}
                               _ {:to      :start
                                  :actions [[:action :found-a :start :via-b-_]]}]
                           _  {:to      :start
                               :actions [[:action :found-a :start :via-_]]}}
             :enter       [[:enter :found-a]]
             :leave       [[:leave :found-a]]
             :stay        [[:stay :found-a]]}})

(deftest actions-test
  (let [count-ab {:states  count-ab-states
                  :action! (fn [type value signal & args]
                             (swap! value conj (into [signal type] args))
                             value)
                  :guard?  (fn [type value signal response & args]
                             (swap! value conj (into [signal type response] args))
                             response)
                  :state   :start
                  :value   (atom [])}]

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
          [\b :guard false :found-a \b]
          [\b :guard true :found-a \b]
          [\b :action :found-a :found-a :via-guard-2]
          [\b :stay :found-a]
          [\a :action :found-a :found-a]
          [\a :stay :found-a]
          [\b :guard false :found-a \b]
          [\b :guard true :found-a \b]
          [\b :action :found-a :found-a :via-guard-2]
          [\b :stay :found-a]
          [\b :guard false :found-a \b]
          [\b :guard true :found-a \b]
          [\b :action :found-a :found-a :via-guard-2]
          [\b :stay :found-a]
          [\a :action :found-a :found-a]
          [\a :stay :found-a]
          [\x :leave :found-a]
          [\x :action :found-a :start :via-_]
          [\x :enter :start]])))
