(ns tilakone.graph.swing
  (:require [dorothy.core :as dot]
            [dorothy.jvm :as v]))

(comment
  (-> (dot/digraph
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
         [:a3 :end {:color :red, :label "Fii"}]
         [:b3 :end]

         [:start {:shape :Mdiamond}]
         [:end {:shape :Msquare}]])
      (dot/dot)
      (v/show!)))