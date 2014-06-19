(ns clue.core
  (:use
   [ring.util.response :only [file-response content-type]])
  (:require
   [net.cgrand.enlive-html :as html]
   [taoensso.timbre :as log]
   [clojure.string :as str]
   [clj-time.core :as t]
   [clj-time.coerce :as t-coerce]
   [clj-time.format :as t-format]))

;; views

(defn wrap-edn-response
  "Middleware that converts responses with a map for a body into a JSON
  response."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (map? (:body response))
        (-> response
            (content-type "application/edn")
            (update-in [:body] pr-str))
        response))))

(defn response [page]
  (let [resp {:status 200
              :headers {"Content-Type" "text/html"}}]
    (if-let [body (:body page)]
      (merge resp page)
      (assoc resp :body page))))

(defn serve-resource [req]
  (-> req :uri (subs 1) clojure.java.io/resource .getPath file-response))

(def ^:dynamic *session*)
(def ^:dynamic *request*)

(defmacro defpage-old [page-name & body]
  (let [request (symbol "request")]
    `(defn ~page-name [~request]
       (response ~@body))))

(defmacro defpage [page-name arg-list & body]
  (let [request 'request
        args 'args]
    `(defn ~page-name [& ~args]
       (fn [~request]
         (binding [*session* (:session ~request)
                   *request* ~request]
           (let ~(vec (apply concat 
                             (map (fn [arg-name i]
                                    [arg-name `(nth ~args ~i)])
                                  arg-list (range))))
             (log/debug (str-pprint *request*))
             (assoc (response ~@body) :session *session*)))))))

(defn set-session! [fn-update & args]
  (set! *session* (apply fn-update (conj args *session*))))

(defmacro maybe-substitute
  ([expr] `(if-let [x# ~expr] (html/substitute x#) identity))
  ([expr & exprs] `(maybe-substitute (or ~expr ~@exprs))))

(defmacro maybe-content
  ([expr] `(if-let [x# ~expr] (html/content x#) identity))
  ([expr & exprs] `(maybe-content (or ~expr ~@exprs))))

(defmacro maybe-append 
  ([expr] `(if-let [x# ~expr] (html/append x#) identity))
  ([expr & exprs] `(maybe-append (or ~expr ~@exprs))))

;; parsing

(defn try-parse-int [str]
  (when (instance? String str)
    (try
      (Integer/parseInt str)
      (catch NumberFormatException e
        nil))))

(def date-parser (t-format/formatter (t/default-time-zone) "MM/dd/YYYY" "YYYY/MM/dd"))

(defn str-to-sql-date [s]
  (when-not (str/blank? s)
    (t-coerce/to-sql-date
     (t-format/parse date-parser s))))

;; random

(def ^:private random (java.util.Random.))

(defn random-nth [coll]
  (nth coll (.nextInt random (count coll))))

(def ^:private char-pool
  (map char (concat (range 48 58) (range 66 92) (range 97 123))))

(def ^:private letter-pool
  (map char (range 97 123)))

(defn random-char []
  (nth char-pool (.nextInt random (count char-pool))))

(defn random-string [length]
  (apply str (take length (repeatedly random-char))))

(defn random-letter []
  (str (nth letter-pool (.nextInt random (count letter-pool)))))

(defn random-int
  ([end] (clojure.core/rand-int end))
  ([start end] (+ start (clojure.core/rand-int (- end start)))))


;; caching

(defmacro defcached [name args form]
  `(def ~name
     (memoize (fn ~args (apply str ~form)))))

;; middlewares

(defn wrap-ns-reload
  "Forces the reloading of each namespace in reloadables on each request. Similar to ring.middleware.reload prior to f39e24da7"
  [app reloadables]
  (fn [req]
    (doseq [ns-sym reloadables]
      (require ns-sym :reload))
    (app req)))

;; collections

(defn distinct-on [getter coll]
  (let [step (fn step [xs seen ]
               (lazy-seq
                ((fn [[f :as xs] seen]
                   (when-let [s (seq xs)]
                     (if (some #(= (getter %)
                                   (getter f))
                               seen)
                       (recur (rest s) seen)
                       (cons f (step (rest s) (conj seen f))))))
                 xs seen)))]
    (step coll [])))

(defn str-pprint [obj]
  (let [w (java.io.StringWriter.)]
    (do
      (clojure.pprint/pprint obj w)
      (.toString w))))
