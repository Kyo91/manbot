(ns manbot.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [manbot.discord :refer [answer-request connect post-message-with-mention]]
            [manbot.poll :as poll]))

(defonce token (slurp "discord-token.txt"))

(def vote-channel (async/chan 10))
(declare command-map)

(defn page-valid? [url]
  (let [response (http/head url {:throw-exceptions? false})]
    (= 200 (:status response))))

(defn valid-pages-for-command [command]
  (when (seq command)
    (let [base-url "https://linux.die.net/man/"]
      (for [i [1 2 3 4 5 6 7 8 "l" "n"]
            :let [url (str base-url i "/" command)]
            :when (page-valid? url)]
        url))))

(defn log-event [type data] 
  (println "\nReceived: " type " -> " data))

(defn man-one [command]
  (first (valid-pages-for-command command)))

(defn man-all [command]
  (when-let [results (valid-pages-for-command command)]
    (String/join "\n" results)))

(defn lmgtfy [phrase]
  (when (seq phrase)
       (let [query (String/join "+" phrase)]
         (str "http://lmgtfy.com/?q=" query))))

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

(defn how-do-i-mute []
  "To mute a channel on desktop enter the channel you want to mute and click the bell at the top right.")

(defn list-commands []
  (let [commands (keys command-map)
        command-list (conj commands "Valid Commands:")]
    (String/join "\n" command-list)))

(defn poll-status []
  (if-not (poll/poll-active?)
    "No active poll."
    (let [header (str "Current results for poll: **" (poll/show-topic) "**")
          results (conj (poll/show-results) header)]
      (String/join "\n" results))))

(def command-map
  {"!man" (fn [d] (man-one (first d)))
   "!manall" (fn [d] (man-all (first d)))
   "!lmgtfy" (fn [d] (lmgtfy d))
   "!mute-channel" (fn [d] (how-do-i-mute))
   "!help" (fn [d] (list-commands))
   "!poll" (fn [d] (start-poll d))
   "!endpoll" (fn [d] (end-poll))
   "!vote" (fn [d] nil)
   "!pollstatus" (fn [d] (poll-status))})

(defn answer-commands [command data]
  (when-let [dispatch (get command-map command)]
    (dispatch data)))

(defn man-requests [type data]
  (answer-request data answer-commands))

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
           {"MESSAGE_CREATE" [man-requests record-vote]
            "MESSAGE_UPDATE" [man-requests]
            "ALL_OTHER" [log-event]
            }
           true))
