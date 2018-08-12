(defproject metosin/tilakone "0.0.0-SNAPSHOT"
  :description "Minimal finite state machine library"
  :dependencies []
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  ; Optionals:
                                  [prismatic/schema "1.1.9"]
                                  [ubergraph "0.5.1"]
                                  ; Test:
                                  [eftest "0.5.2"]
                                  [metosin/testit "0.4.0-SNAPSHOT"]
                                  ; Perf test:
                                  [criterium "0.4.4"]
                                  [reduce-fsm "0.1.4"]]
                   :source-paths ["examples"]}
             :perf {:jvm-opts ^:replace ["-server"
                                         "-Xms4096m"
                                         "-Xmx4096m"
                                         "-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[lein-eftest "0.5.2"]]
  :eftest {:multithread? false}
  :test-selectors {:default (constantly true)
                   :all (constantly true)}
  :aliases {"perf" ["with-profile" "default,dev,perf"]
            "run-perf" ["perf" "run" "-m" "example.perf-testing"]}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"})

