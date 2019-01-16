(defproject metosin/tilakone.root "0.0.0"
  :description "Minimal finite state machine library"
  :dependencies []
  :source-paths ["dev"
                 "modules/core/src"
                 "modules/schema/src"
                 "modules/graph/src"
                 "examples"]
  :test-paths ["modules/core/test"
               "modules/schema/test"
               "modules/graph/test"]
  :profiles {:dev  {:dependencies [[org.clojure/clojure "1.10.0"]
                                   ; Dev workflow:
                                   [org.clojure/tools.namespace "0.2.11"]
                                   ; Module deps:
                                   [prismatic/schema "1.1.9"]
                                   [dorothy "0.0.7"]
                                   ; Test:
                                   [eftest "0.5.4"]
                                   [metosin/testit "0.4.0-SNAPSHOT"]
                                   ; Perf test:
                                   [criterium "0.4.4"]
                                   [reduce-fsm "0.1.4"]]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xms2g"
                                         "-Xmx2g"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[lein-eftest "0.5.4"]]
  :eftest {:multithread? false
           :report       eftest.report.pretty/report}
  :test-selectors {:default (constantly true)
                   :all     (constantly true)}
  :aliases {"perf"      ["with-profile" "default,dev,perf"]
            "perf-test" ["perf" "run" "-m" "example.perf-test"]}
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"})
