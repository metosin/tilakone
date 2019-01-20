# Unreleased  Breaking changes

Changed internal state-machine representation, previously context had states, state, etc under `:tilakone.core/process` 
key. This superfluous key is now removed, all data is now at top level.

The "top level" was sometimes named as `ctx`, now it's consistently named as `fsm` everywhere.
    
Breaking change: `match?` now accepts two arguments, the signal and the `:tilakone.core/on` of the transition, and is
expected to return truthy if the signal matches the second argument.


# 0.0.4  (2019/01/19)  Breaking changes

All used keywords are now qualified keywords, using the `tilakone.core` namespace. For example, previously states
were under key `:states`, from this release onwards the states are under key `:tilakone.core/states`. This is
expected to make extending tilakone easier. 

Prior this release, the `:value` has special meaning. In this release `:value` is not significant. This is expected
to help using tilakone in cases that naturally use different keys for "value", like `:request` and `:response`.

The `:tilakone.core/action!` (previously `:action!`) was expected to return a new value for `:value`. From this release
onward `:tilakone.core/action!` is expected to return the same data structure as it receives, with some data possibly
updated.


# 0.0.3  (2019/01/16)  Breaking changes

Pre 0.0.3 release the guard?, action! and match? functions got three arguments, `value`, `signal` and the data of
the guard, action or signal. In this release all three functions are called with just one argument, and map with 
`:process`, `:signal` and either `:guard`, `:action`, or `:on`, respectively.

Previously, first transition that matched the signal was selected. If one or more guards rejected that transition, the 
signal was rejected. From 0.0.3 onward all possible transitions are considered, and the first transition that is 
accepted by all relevant guards is selected.
