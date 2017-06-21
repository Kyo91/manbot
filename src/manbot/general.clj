(ns manbot.general)

(defn lmgtfy [phrase]
  (when (seq phrase)
    (let [query (String/join "+" phrase)]
      (str "http://lmgtfy.com/?q=" query))))

(defn how-do-i-mute []
  "To mute a channel on desktop enter the channel you want to mute and click the bell at the top right.")

