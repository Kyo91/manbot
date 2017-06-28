(defproject manbot "0.3.2"
  :description "A very broken discord bot."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.6.1"]
                 [org.clojure/data.json "0.2.6"]
                 [stylefruits/gniazdo "1.0.0"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.analyzer "0.6.9"]
                 [org.clojure/tools.analyzer.jvm "0.6.9"]
                 [org.clojure/core.async "0.3.443"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main ^:skip-aot manbot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
