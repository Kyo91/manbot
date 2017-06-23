(ns manbot.man
  (:require [manbot.general :refer [page-valid?]]))

(defn valid-pages-for-command [command]
  (when (seq command)
    (let [base-url "https://linux.die.net/man/"]
      (for [i [1 2 3 4 5 6 7 8 "l" "n"]
            :let [url (str base-url i "/" command)]
            :when (page-valid? url)]
        url))))


(defn man-one [command]
  (first (valid-pages-for-command command)))

(defn man-all [command]
  (when-let [results (valid-pages-for-command command)]
    (String/join "\n" results)))
