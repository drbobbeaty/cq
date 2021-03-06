(ns cq.server
  "The routes for the web server."
  (:require [compojure
              [core :refer [defroutes GET POST]]
              [handler :as handler]
              [route :as route]]
            [clj-time.coerce :refer [to-long from-long]]
            [clj-time.core :refer [now]]
            [clojure.tools.logging :refer [infof warn warnf error errorf]]
            [cheshire.core :as json]
            [cq.block :as blk]
            [ring.middleware.jsonp :refer [wrap-json-with-padding]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :as resp]
            [ring.util.io :refer [piped-input-stream]])
  (:import [java.io BufferedWriter OutputStreamWriter IOException]))

(extend-protocol cheshire.generate/JSONable
  org.joda.time.DateTime
  (to-json [t jg]
    (cheshire.generate/write-string jg (str t))))

(defn git-commit
  "Tries to determine the currently deployed commit by looking for it
  in the name of the jar. Returns nil if it cannot be determined."
  []
  (-> clojure.lang.RT
      .getProtectionDomain
      .getCodeSource
      .getLocation
      .getPath
      java.io.File.
      .getName
      (->> (re-find #"-([a-f0-9]{5,})\.jar"))
      second))

(defn return-code
  "Creates a ring response for returning the given return code."
  [code]
  {:status code
   :headers {"Content-Type" "application/json; charset=UTF-8"}})

(defn return-json
  "Creates a ring response for returning the given object as JSON."
  ([ob] (return-json ob (now) 200))
  ([ob lastm] (return-json ob lastm 200))
  ([ob lastm code]
    {:status code
     :headers {"Content-Type" "application/json; charset=UTF-8"
               "Access-Control-Allow-Origin" "*"
               "Last-Modified" (str (or lastm (now)))}
     :body (piped-input-stream
             (bound-fn [out]
               (with-open [osw (OutputStreamWriter. out)
                           bw (BufferedWriter. osw)]
                 (let [error-streaming
                       (fn [e]
                         ;; Since the HTTP headers have already been sent,
                         ;; at this point it is too late to report the error
                         ;; as a 500. The best we can do is abruptly print
                         ;; an error and quit.
                         (.write bw "\n\n---CRYPTOQUIP SERVICE ERROR WHILE STREAMING JSON---\n")
                         (.write bw (str e "\n\n"))
                         (warnf "Streaming exception for JSONP: %s" (.getMessage e)))]
                   (try
                     (json/generate-stream ob bw)
                     ;; Handle "pipe closed" errors
                     (catch IOException e
                       (if (re-find #"Pipe closed" (.getMessage e))
                         (infof "Pipe Closed exception: %s" (.getMessage e))
                         (error-streaming e)))
                     (catch Throwable t
                       (error-streaming t)))))))}))

(defroutes app-routes
  "Primary routes for the webserver."
  (GET "/" []
    (resp/redirect "/index.html"))
  (GET "/info" []
    (return-json {:app "cryptoquip solver",
                  :hello? "World!",
                  :code (or (git-commit) "unknown commit")}))
  (GET "/heartbeat" []
    (return-code 200))
  ;; simple test for the default quip - just for testing
  (GET "/benchmark" []
    (let [quip "fict o ncc bivteclnbklzn o lcpji ukl pt vzglcddp"
          clue {\b \t}
          ans (blk/solve quip clue)]
      (return-json {:cyphertext quip
                    :clue (into {} (for [[k v] clue] [(str k) (str v)]))
                    :plaintext ans})))
  ;; let's get the quip and clue and generate an answer
  (POST "/solve" [:as {body :body}]
    (let [cfg (json/parse-string (slurp body) true)
          quip (:cyphertext cfg)
          clue (into {} (for [[k v] (:clue cfg)] [(first (name k)) (first v)]))]
      (infof "got quip: |%s| w/clue: %s" quip clue)
      (if (and (string? quip) (map? clue))
        (return-json {:cyphertext quip
                      :clue (into {} (for [[k v] clue] [(str k) (str v)]))
                      :plaintext (blk/solve quip clue)})
        (return-code 400))))
  ;; Finish up with the static resources and the 404 page
  (route/resources "/")
  (route/not-found "<h1>Page not Found!</h1>"))

(defn wrap-logging
  "Ring middleware to log requests and exceptions."
  [handler]
  (fn [req]
    (infof "Handling request: %s" (:uri req))
    (try (handler req)
         (catch Throwable t
           (error t "Server error!")
           (throw t)))))

(def app
  "The actual ring handler that is run -- this is the routes above
   wrapped in various middlewares."
  (-> app-routes
      wrap-json-with-padding
      handler/site
      wrap-params
      wrap-logging))
