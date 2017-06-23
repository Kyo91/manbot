(ns manbot.core
  (:gen-class)
  (:require [clojure.core.async :as async]
            [clojure.string :as str]
            [manbot.discord :refer [answer-request connect]]
            [manbot.discord-async :as da]
            [manbot.poll :as poll]
            [manbot.general :as general]
            [manbot.man :as man]
            [clojure.tools.logging :as log]))

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

(defn record-vote [data]
  (let [id (get-in data ["author" "id"])
        [command other] (str/split (get data "content" "") #" " 2)
        payload {:id id :vote other}]
    (when (seq other)
      (async/go (async/>! vote-channel payload))
      ; nil to suppress message.
      nil)))

(defn poll-status []
  (if-not (poll/poll-active?)
    "No active poll."
    (let [header (str "Current results for poll: **" (poll/show-topic) "**")
          results (conj (poll/show-results) header)]
      (String/join "\n" results))))

(def content-commands
  [["!man" (fn [d] (man/man-one (first d))) true]
   ["!man-all" (fn [d] (man/man-all (first d))) true]
   ["!lmgtfy" (fn [d] (general/lmgtfy d))]
   ["!mute-channel" (fn [d] (general/how-do-i-mute))]
   ["!xkcd" (fn [d] (general/xkcd (first d))) true]
   ["!help" (fn [d] (list-commands)) true]
   ["!poll" (fn [d] (start-poll d))]
   ["!end-poll" (fn [d] (end-poll))]
   ["!poll-status" (fn [d] (poll-status)) true]])

(def payload-commands
  [["!vote" record-vote]])

(def bot-commands "All vaid commands for the bot."
  (concat content-commands payload-commands))

(defn register-with [registrar command-vec]
  (doseq [[command f mention?] command-vec]
    (registrar command f mention?)))

(defn register-commands []
  (register-with da/register-message-content content-commands)
  (register-with da/register-on-message payload-commands))

(defn list-commands []
  (let [commands (map first bot-commands)
        command-list (conj commands "Valid Commands:")]
    (String/join "\n" command-list)))

(defn -main
  "Start up bot"
  [& args]
  (log/info "Starting up bot....")
  (poll/accumulate-votes vote-channel)
  (register-commands)
  (da/connect token true))
