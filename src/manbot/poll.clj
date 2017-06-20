(ns manbot.poll
  (:require [clojure.core.async]))

(defonce active-poll (atom nil))
(defonce voters (atom nil))


(defn poll-active? []
  (seq @active-poll))

(defn start-poll [topic]
  (when-not (poll-active?)
    (reset! active-poll {:topic topic :votes {}})
    (reset! voters {})))

(defn swap-vote! [f vote not-found]
  (swap! active-poll
         #(assoc-in % [:votes vote]
                    (f (get-in % [:votes vote] not-found)))))

(defn add-vote [userid vote]
  (when (poll-active?)
    (when-let [oldvote (get @voters userid)]
      (swap-vote! dec oldvote 1))
    (swap-vote! inc vote 0)
    (swap! voters #(assoc % userid vote))))

(defn show-results []
  (when (poll-active?)
    (let [votes (:votes @active-poll)
        sorted (into (sorted-map-by
                      (fn [key1 key2]
                        (compare [(get votes key2) key2]
                                 [(get votes key1) key1])))
                     votes)]
    (map (fn [[vote count]] (str vote ":\t" count)) sorted))))

(defn show-topic []
  (when (poll-active?)
    (:topic @active-poll)))

(defn end-poll []
  (when (poll-active?)
    (let [results (show-results)]
    (reset! active-poll nil)
    (reset! voters nil)
    results)))
