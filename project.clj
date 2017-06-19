(defproject manbot "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.6.1"]
                 [org.clojure/data.json "0.2.6"]
                 [stylefruits/gniazdo "1.0.0"]
                 [org.clojure/core.match "0.2.2"]
                 [environ "1.1.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [hickory "0.7.1"]]
  :main ^:skip-aot manbot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
