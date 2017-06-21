(ns manbot.core
  (:gen-class)
  (:require [clojure.core.async :as async]
            [clojure.string :as str]
            [manbot.discord :refer [answer-request connect]]
            [manbot.poll :as poll]
            [manbot.general :as general]
            [manbot.man :as man]))

(declare list-commands)

(defonce token (slurp "discord-token.txt"))
(def vote-channel (async/chan 10))

(defn log-event [type data] 
  (println "\nReceived: " type " -> " data))

(defn start-poll [topic-coll]
  (if-not (seq topic-coll)
    "No topic specified for poll"
    (let [topic (String/join " " topic-coll)]
      (if (poll/start-poll topic)
        (str "Started poll: **" topic "**")
        (str "Poll already started: **" (poll/show-topic) "**")))))

(defn end-poll []
  (if-let [final-results (poll/end-poll)]
    (String/join "\n" final-results)
    "No poll in progress"))

(defn poll-status []
  (if-not (poll/poll-active?)
    "No active poll."
    (let [header (str "Current results for poll: **" (poll/show-topic) "**")
          results (conj (poll/show-results) header)]
      (String/join "\n" results))))

(def command-map
  {"!man" (fn [d] (man/man-one (first d)))
   "!manall" (fn [d] (man/man-all (first d)))
   "!lmgtfy" (fn [d] (general/lmgtfy d))
   "!mute-channel" (fn [d] (general/how-do-i-mute))
   "!help" (fn [d] (list-commands))
   "!poll" (fn [d] (start-poll d))
   "!endpoll" (fn [d] (end-poll))
   "!vote" (fn [d] nil)
   "!pollstatus" (fn [d] (poll-status))})

(defn list-commands []
  (let [commands (keys command-map)
        command-list (conj commands "Valid Commands:")]
    (String/join "\n" command-list)))


(defn command-dispatch [command data]
  (when-let [dispatch (get command-map command)]
    (dispatch data)))

(defn respond-request [type data]
  (answer-request data command-dispatch))

(defn record-vote [type data]
  (let [id (get-in data ["author" "id"])
        [command other] (str/split (get data "content" "") #" " 2)
        payload {:id id :vote other}]
    (when (and (= command "!vote") (seq other))
      (async/go (async/>! vote-channel payload)))))

(defn -main
  "Start up bot"
  [& args]
  (println "Starting up bot....")
  (poll/accumulate-votes vote-channel)
  (connect token
           {"MESSAGE_CREATE" [respond-request record-vote]
            "MESSAGE_UPDATE" [respond-request]
            "ALL_OTHER" [log-event]
            }
           true))

(defn list-commands []
  (let [commands (keys command-map)
        command-list (conj commands "Valid Commands:")]
    (String/join "\n" command-list)))
