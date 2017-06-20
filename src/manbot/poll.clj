(ns manbot.poll
  (:require [clojure.core.async :refer [go-loop <!]]))

(defonce poll (atom nil))
(defonce voters (atom nil))

(defn poll-active? []
  (seq @poll))

(defn start-poll [topic]
  (when-not (poll-active?)
    (reset! poll {:topic topic :votes {}})
    (reset! voters {})))

(defn swap-vote! [f vote not-found]
  (swap! poll
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
    (let [votes (:votes @poll)
        sorted (into (sorted-map-by
                      (fn [key1 key2]
                        (compare [(get votes key2) key2]
                                 [(get votes key1) key1])))
                     votes)]
    (map (fn [[vote count]] (str vote ":\t" count)) sorted))))

(defn show-topic []
  (when (poll-active?)
    (:topic @poll)))

(defn prune-empty []
  (when (poll-active?)
    (let [oldvotes (:votes @poll)
          newvotes (filter (fn [[k v]] (not= 0 v)) oldvotes)]
      (swap! poll #(assoc % :votes (into {} newvotes))))))

(defn end-poll []
  (when (poll-active?)
    (let [title (str (show-topic) "  Poll Results:")
          results (conj (show-results) title)]
      (reset! poll nil)
      (reset! voters nil)
      results)))

(defn accumulate-votes [vote-chan]
  (go-loop [vote-map (<! vote-chan)]
    (let [userid (:id vote-map)
          vote (:vote vote-map)]
      (add-vote userid vote)
      (prune-empty)
      (recur (<! vote-chan)))))

