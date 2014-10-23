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

(def ws-port  (atom 0))
(def out-file (atom nil))
(def continue (atom nil))
(def event    (atom nil))

(def deps 
  (concat
    (whendep cemerick.piggieback   [[com.cemerick/piggieback   "0.1.3"]])
    (whendep weasel.repl.websocket [[weasel                    "0.4.1"]])
    (whendep cljs.analyzer         [[org.clojure/clojurescript "0.0-2371"]])))

(defn start-repl
  []
  (with-out-str
    ((r cemerick.piggieback/cljs-repl)
     :repl-env ((r weasel.repl.websocket/repl-env) :port @ws-port)))
  (let [conn (->> @@(r weasel.repl.server/state)
               :server meta :local-port (format "ws://localhost:%d"))]
    (info "<< started Weasel server on %s >>\n\n" conn)
    (io/make-parents @out-file)
    (->> (template
           ((ns tailrecursion.boot-cljs-repl
              (:require [weasel.repl :as repl]))
            (when-not (repl/alive?) (repl/connect ~conn))))
      (map pr-str) (interpose "\n") (apply str) (spit @out-file))
    (@continue (make-event @event))))

(deftask cljs-repl
  "Start a ClojureScript REPL server.

  The default configuration starts a websocket server on a random available
  port on localhost."

  [p port PORT int "The port the websocket server listens on."]

  (let [src (mksrcdir!)]
    (when port (reset! ws-port port))
    (when (seq deps) (set-env! :dependencies #(into % deps)))
    (reset! out-file (io/file src "tailrecursion" "boot_cljs_repl.cljs"))
    (comp
      (repl
        :server     true
        :middleware [(r cemerick.piggieback/wrap-cljs-repl)])
      (fn [continue*]
        (fn [event*]
          (reset! event event*)
          (reset! continue continue*)
          (continue* event*))))))
