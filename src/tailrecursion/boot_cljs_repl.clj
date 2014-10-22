(ns tailrecursion.boot-cljs-repl
  {:boot/export-tasks true}
  (:require
   [clojure.java.io    :as    io]
   [boot.pod           :as    pod]
   [boot.util          :refer :all]
   [boot.core          :refer :all]
   [boot.task.built-in :refer :all]
   [boot.from.backtick :refer [template]]))

(defmacro r
  [sym]
  `(do (require '~(symbol (namespace sym))) (resolve '~sym)))

(defmacro whendep
  [ns coord]
  `(when-not (guard (do (require '~ns) :ok)) '~coord))

(def +ws-port+ (atom 9001))

(def deps 
  (concat
    (whendep cemerick.piggieback   [[com.cemerick/piggieback   "0.1.3"]])
    (whendep weasel.repl.websocket [[weasel                    "0.4.1"]])
    (whendep cljs.analyzer         [[org.clojure/clojurescript "0.0-2371"]])))

(defn start-repl
  []
  ((r cemerick.piggieback/cljs-repl)
   :repl-env ((r weasel.repl.websocket/repl-env) :port @+ws-port+)))

(deftask cljs-repl
  "Start a ClojureScript REPL server.

  The default configuration starts a websocket server on localhost:9001."

  [p port PORT int "The port the websocket server listens on."]

  (let [src (mksrcdir!)
        ws  (str "ws:localhost:" (or port @+ws-port+))
        out (io/file src "tailrecursion" "boot_cljs_repl.cljs")]
    (io/make-parents out)
    (when port (reset! +ws-port+ port))
    (when (seq deps) (set-env! :dependencies #(into % deps)))
    (comp
      (with-pre-wrap
        (info "ClojureScript REPL configured for %s...\n" ws)
        (->> (template
               ((ns tailrecursion.boot-cljs-repl
                  (:require [weasel.repl :as repl]))
                (when-not (repl/alive?) (repl/connect ~ws))))
          (map pr-str) (interpose "\n") (apply str) (spit out)))
      (repl
        :server     true
        :middleware [(r cemerick.piggieback/wrap-cljs-repl)]))))
