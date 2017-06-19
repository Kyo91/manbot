(ns manbot.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [manbot.discord :refer [answer-request connect disconnect answer-command]]
            [clojure.core.match :refer [match]]
            [environ.core :refer [env]]))

(defonce token (slurp "discord-token.txt"))

(defn page-valid? [url]
  (let [response (http/get url {:throw-exceptions? false})]
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

(defn answer-commands [command data]
  (match command
         "!man" (man-one (first data))
         "!manall" (man-all (first data))
         :else nil))

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
