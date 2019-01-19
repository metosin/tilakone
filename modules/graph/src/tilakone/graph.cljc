(ns tilakone.graph
  (:require [dorothy.core :as dot]
            [dorothy.jvm :as v]))

(defn transition->name [{:keys [name on]}]
  (format "[%s] %s"
          (if (= on :tilakone.core/_)
            "_"
            (pr-str on))
          (or name "")))

(defn states->edges [states]
  (mapcat (fn [state]
            (let [state-name (:name state)]
              (->> state
                   :transitions
                   (map (fn [{:keys [to] :as transition}]
                          [state-name (or to state-name) {:label (transition->name transition)}])))))
          states))

(defn states->nodes [states]
  (map (fn [state]
         (let [state-name (:name state)]
           [state-name {:shape :box}]))
       states))

(comment
  (require '[dorothy.jvm :as v])
  (def _ :tilakone.core/_)
  (def states [{:name        :start
                :transitions [{:on \a
                               :to :found-a}
                              {:on _}]}
               {:name        :found-a
                :transitions [{:on \a}
                              {:on \b
                               :to :start
                               :actions [:inc-val]}
                              {:on _
                               :to :start}]}])
  (->> (concat (states->nodes states)
               (states->edges states))
       (dot/digraph {})
       (dot/dot)
       (v/show!)))

(comment
  (dot/digraph
    [(dot/subgraph :cluster_0 [{:style :filled, :color :lightgrey, :label "process #1"}
                               (dot/node-attrs {:style :filled, :color :white})
                               [:a0 :> :a1 :> :a2 :> :a3]])

     (dot/subgraph :cluster_1 [{:color :blue, :label "process #2"}
                               (dot/node-attrs {:style :filled})
                               [:b0 :> :b1 :> :b2 :> :b3]])

     [:start :a0]
     [:start :b0]
     [:a1 :b3]
     [:b2 :a3]
     [:a3 :a0]
     [:a3 :end]
     [:b3 :end]

     [:start {:shape :Mdiamond}]
     [:end {:shape :Msquare}]]))



