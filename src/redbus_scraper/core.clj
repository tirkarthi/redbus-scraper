(ns redbus-scraper.core
  (:gen-class)
  (:require [cheshire.core :refer :all]
            [clj-time
             [core :as t]
             [format :as f]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [monger
             [collection :as mc]
             [core :as mg]
             [credentials :as mcr]]
            [org.httpkit.client :as http]
            [overtone.at-at :as overtone]))

(def my-pool (overtone/mk-pool))

(defn generate-date-range
  "Generate date range of the form dd-MMM-yyyy from today to <interval> days from today"
  []
  (let [interval (env :redbus_scraper_interval 1)]
    (->> (range interval)
         (map #(t/plus (t/today) (t/days %1)))
         (map #(f/unparse-local (f/formatter "dd-MMM-yyyy") %1)))))

(defn generate-payload
  [locations]
  (let [date-range (generate-date-range)]
    (for [location locations
          doj date-range]
      (assoc location :doj doj))))

(defn download-json
  "Download the json from url with query params and then send the response and src id"
  [base-url {:keys [src dest src-id dest-id doj]}]
  (let [query-params {:src src :dst dest :fromCity src-id :toCity dest-id :doj doj}
        req (http/get base-url {:query-params query-params})
        {:keys [status headers body error] :as resp} @req
        now (quot (System/currentTimeMillis) 1000)]
    (if error
      (println "Failed, exception: " error)
      {:src-id src-id :dest-id dest-id :doj doj :now now :content body})))

(defn write-to-mongodb
  [coll data now]
  (let [admin-db "admin"
        user (env :redbus-mongo-user "admin")
        password (.toCharArray (env :redbus-mongo-password "nicetry"))
        creds (mcr/create user admin-db password)
        host (env :redbus-mongo-host "127.0.0.1")
        conn (mg/connect-with-credentials host creds)
        db (mg/get-db conn (env :redbus-database "redbus"))]
    (doall (map #(mc/insert db coll (assoc %1 :created-at now)) data))
    (println "Wrote to mongodb")))

(defn generate-name
  [src-id dest-id doj]
  (let [prefix "data"]
    (->
     (str/join "_" [prefix src-id dest-id doj])
     (str/replace "-" "_"))))

(defn dump-json
  "Dump the json to path of the form srcid_destid_doj_timestamp.json
  Also store the json to mongo db collection of the form srcid_destid_doj"
  [{:keys [src-id dest-id doj now content]}]
  (let [name  (generate-name src-id dest-id doj)
        filepath (str (env :redbus-base-path "/home/ubuntu/") name "_" now ".json")
        coll name
        content (parse-string content true)
        buses (get-in content [:SRD 0 :RIN 0 :InvList])
        ac-buses (->> buses
                      (filter #(get-in % [:BusCategory :IsAc]))
                      (sort-by #(get-in % [:MinFare]) >))
        ac-buses (map #(dissoc %1 :BPLst :param42 :DPLst) ac-buses)]

    (when (seq ac-buses)
      (write-to-mongodb coll ac-buses now)
      (generate-stream ac-buses (clojure.java.io/writer filepath))
      (println (str "Wrote json to " filepath)))))

(defn scrape-data
  []
  (let [config (parse-string (slurp (env :config-file-path "config.json")) true)
        url (:url config)
        locations (:locations config)
        payloads (generate-payload locations)]
    (doall
     (->> payloads
          (map #(download-json url %))
          (map dump-json)))))

(defn -main
  [& args]
  (println "Started scraping data")
  (overtone/every (* 1000 60 15) #(scrape-data) my-pool))
