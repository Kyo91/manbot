(ns manbot.discord-async
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.core.async :as async]
            [gniazdo.core :as ws]))

(defonce the-token (atom nil))
(defonce the-gateway (atom nil))
(defonce the-socket (atom nil))
(defonce the-heartbeat-interval (atom nil))
(defonce the-keepalive (atom false))
(defonce the-seq (atom nil))
(defonce reconnect-needed (atom false))

(def message-chan (async/chan))

(defn find-command [payload]
  (let [content (get payload "content")]
    (when (and (seq content) (= \! (String/.charAt content 0)))
      (first (str/split content #" ")))))

(defn content-minus-command [content]
  (when (seq content)
    (let [[command & other] (str/split content #" ")]
      other)))

(def message-pub
  (async/pub message-chan find-command))

(defn event-name-to-keyword [name]
  (when (seq name)
    (-> name .toLowerCase (.replace "_" "-") keyword)))

(defn disconnect []
  (reset! reconnect-needed false)
  (reset! the-keepalive false)
  (if (not (nil? @the-socket)) (ws/close @the-socket))
  (reset! the-token nil)
  (reset! the-gateway nil)
  (reset! the-socket nil)
  (reset! the-seq nil)
  (reset! the-heartbeat-interval nil))

(defn connect [token log-events?]
  (disconnect)
  (reset! the-keepalive true)
  (reset! the-token (str "Bot " token))
  (reset! the-gateway (str
                        (get
                          (json/read-str
                            (:body (http/get "https://discordapp.com/api/gateway"
                                             {:headers {:authorization @the-token}})))
                          "url")
                        "?v=6&encoding=json"))
  (reset! the-socket
          (ws/connect
            @the-gateway
            :on-error #(log/warn %)
            :on-receive #(let [received (json/read-str %)
                               logevent (if log-events? (log/debug "\n" %))
                               op (get received "op")
                               type (event-name-to-keyword (get received "t"))
                               payload (get received "d")
                               seq (get received "s")]
                           (if (= 10 op) (reset! the-heartbeat-interval (get payload "heartbeat_interval")))
                           (when-not (nil? seq) (reset! the-seq seq))
                           (when-not (nil? type)
                             (async/go (async/>! message-chan payload))))))
  (.start (Thread. (fn []
                     (try
                       (while @the-keepalive
                         (if (nil? @the-heartbeat-interval)
                           (Thread/sleep 100)
                           (do
                             (if log-events? (println "\nSending heartbeat " @the-seq))
                             (ws/send-msg @the-socket (json/write-str {:op 1, :d @the-seq}))
                             (Thread/sleep @the-heartbeat-interval)
                             )))
                       (catch Exception e (do
                                            (println "\nCaught exception: " (.getMessage e))
                                            (reset! reconnect-needed true)
                                            ))))))
  (Thread/sleep 1000)
  (ws/send-msg @the-socket (json/write-str {:op 2, :d {"token" @the-token
                                                       "properties" {"$os" "linux"
                                                                     "$browser" "clj-discord"
                                                                     "$device" "clj-discord"
                                                                     "$referrer" ""
                                                                     "$referring_domain" ""}
                                                       "compress" false}}))
  (while (not @reconnect-needed) (Thread/sleep 1000))
  (recur token log-events?))

(defn post-message [channel-id message]
  (http/post (str "https://discordapp.com/api/channels/" channel-id "/messages")
             {:body (json/write-str {:content message
                                     :nonce (str (System/currentTimeMillis))
                                     :tts false})
              :headers {:authorization @the-token}
              :content-type :json
              :accept :json}))

(defn post-message-with-mention [channel-id message user-id]
  (log/info "Starting post-with-mention with parameters: "
            "\nChannel ID: " channel-id
            "\nMessage: " message
            "\nUser ID: " user-id)
  (post-message channel-id (str "<@" user-id ">" message)))


(defn register-on-message
  "Register a function which acts on the payload from a discord message starting with the provided command."
  [command-name function & [mention?]]
  (let [subscription (async/chan)]
    (async/sub message-pub command-name subscription)
    (async/go-loop []
      (let [data (async/<! subscription)
            message (function data)
            channel-id (get data "channel_id")
            user-id (get-in data ["author" "id"])]
        (when (seq message)
          (if mention?
            (post-message-with-mention channel-id message user-id)
            (post-message channel-id message)))))))

(defn register-message-content
  "A convenience function for registering functions that only need the message content (minus command)."
  [command-name function & [mention?]]
  (register-on-message command-name (fn [payload] (-> payload
                                                     (get "content")
                                                     content-minus-command
                                                     function)) mention?))
