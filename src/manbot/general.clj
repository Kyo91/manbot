(ns manbot.general
  (:require [clj-http.client :as http]))

(defn lmgtfy [phrase]
  (when (seq phrase)
    (let [query (String/join "+" phrase)]
      (str "http://lmgtfy.com/?q=" query))))

(defn how-do-i-mute []
  "To mute a channel on desktop enter the channel you want to mute and click the bell at the top right.")

(defn page-valid? [url]
  (let [response (http/head url {:throw-exceptions? false})]
    (= 200 (:status response))))

(defn xkcd [id]
  (let [num (if id (str "/" id "/") "")
        url (str "https://xkcd.com" num)]
    (if (page-valid? url)
      url
      (str "No xkcd found for: " id))))
