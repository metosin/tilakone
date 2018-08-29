(ns tilakone.graph
  (:require [dorothy.core :as dot]
            [dorothy.jvm :as v]))

(comment

  ;
  ; The FSM looks like this:
  ;

  (def FSM
    {:states  [{:name        Any ; State name (can be string, keyword, symbol, any clojure value)
                :desc        Str ; State description
                :transitions [{:name    Any ; Transition name
                               :desc    Str ; Transition description
                               :to      Any ; Name of the next state
                               :on      Matcher ; Data for match?, does the signal match this transition?
                               :guards  [Guard] ; Data for guard?, is this transition allowed?
                               :actions [Action]}] ; Actions to be performed on this transition
                :enter       [Action] ; Actions to be performed when entering this state
                :leave       [Action] ; Actions to be performed when leaving this state
                :stay        [Action]}] ; Actions to be performed when signal is processed, but state remains the same
     :match?  (fn [value signal matcher] ... true/false) ; Signal matching predicate
     :guard?  (fn [value signal guard] ... true/false) ; Guard matching predicate
     :action! (fn [value signal action] ... value) ; Action function
     :state   Any ; Current state
     :value   Any}) ; Current value

  )


(defn states->dot [states]
  (->> states
       (mapcat (fn [state]
                 (let [state-name (:name state)]
                   (->> state
                        :transitions
                        (map (fn [{:keys [to name]}]
                               [state-name to {:label name}]))))))
       (dot/digraph {})
       (dot/dot)))

(comment
  (require '[dorothy.jvm :as v])
  (def states [{:name :start
                :transitions [{:name :submit
                               :to :}]}])
  (-> states states->dot v/show!))

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
