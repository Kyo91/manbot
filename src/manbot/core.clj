(ns manbot.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [manbot.discord :refer [answer-request connect post-message-with-mention]]
            [manbot.poll :as poll]))

(defonce token (slurp "discord-token.txt"))

(def vote-channel (chan 10))

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

(defn start-poll [topic]
  (if-not (seq topic)
    "No topic specified for poll"
    (if (poll/start-poll (String/join " " topic))
      (str "Started poll: " topic)
      (str "Poll already started: " (poll/show-topic)))))

(defn end-poll []
  (if-let [final-results (poll/end-poll)]
    (String/join "\n" final-results)
    "No poll in progress"))

(defn answer-commands [command data]
  (match command
         "!man" (man-one (first data))
         "!manall" (man-all (first data))
         "!lmgtfy" (lmgtfy data)
         "!poll" (start-poll data)
         "!endpoll" (end-poll)
         :else nil))


(defn man-requests [type data]
  (answer-request data answer-commands))

(defn record-vote [type data]
  (let [id (get-in data ["author" "id"])
        [command other] (str/split (get data "content" "") #" " 2)
        payload {:id id :vote other}]
    (when (and (= command "!vote") (seq other))
      (go (async/>! vote-channel payload)))))

(defn -main
  "Start up bot"
  [& args]
  (println "Starting up bot....")
  (poll/accumulate-votes vote-channel)
  (connect token
           {"MESSAGE_CREATE" [man-requests]
            "MESSAGE_UPDATE" [man-requests]
            "ALL_OTHER" [log-event]
            }
           true))
