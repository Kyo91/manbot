(ns manbot.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [manbot.discord :refer [answer-request connect disconnect answer-command]]
            [clojure.core.match :refer [match]]
            [environ.core :refer [env]]
            [hickory.core :refer [parse as-hickory]]
            [hickory.select :as s]))

(defonce token (slurp "discord-token.txt"))

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

(defn how-do-i-mute []
  "To mute a channel on desktop enter the channel you want to mute and click the bell at the top right.")

(defn list-commands []
  (let [commands (keys command-map)
        command-list (conj commands "Valid Commands:")]
    (String/join "\n" command-list)))

(def command-map
  {"!man" (fn [d] (man-one (first d)))
   "!manall" (fn [d] (man-all (first d)))
   "!lmgtfy" (fn [d] (lmgtfy d))
   "!mute-channel" (fn [d] (how-do-i-mute))
   "!help" (fn [d] (list-commands))})

(defn answer-commands [command data]
  (when-let [dispatch (get command-map command)]
    (dispatch data)))

(defn man-requests [type data]
  (answer-request data answer-commands))

(defn -main
  "Start up bot"
  [& args]
  (println "Starting up bot....")
  (connect token
           {"MESSAGE_CREATE" [man-requests]
            "MESSAGE_UPDATE" [man-requests]
            "ALL_OTHER" [log-event]
            }
           true))
