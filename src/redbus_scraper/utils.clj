(ns redbus-scraper.utils)

(defn print-item
  [item]
  (let [fmt "%1$-35s | %2$10s | %3$10s | %4$4s | %5$5s"]
    (println (apply format fmt item))))

(defn select-keys* [m paths]
  (into {} (map (fn [p]
                  [(last p) (get-in m p)]))
        paths))

;; {ac-sleeper true ac-seater false}
;; (->> ac-buses
;;      (group-by #(get-in %1 [:BusCategory :IsSleeper])))
