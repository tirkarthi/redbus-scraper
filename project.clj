(defproject redbus-scraper "0.1"
  :description "A simple redbus scraper that dumps data to MongoDB for analysis"
  :url "https://github.com/tirkarthi/redbus-scraper"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [com.novemberain/monger "3.1.0"]
                 [http-kit "2.2.0"]
                 [cheshire "5.7.0"]
                 [overtone/at-at "1.2.0"]
                 [environ "1.1.0"]
                 [clj-time "0.13.0"]
                 [org.slf4j/slf4j-nop "1.7.12"]]
  :main ^:skip-aot redbus-scraper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
