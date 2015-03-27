(ns adzerk.boot-cljs-repl
  {:boot/export-tasks true}
  (:require
   [clojure.java.io    :as    io]
   [boot.pod           :as    pod]
   [boot.util          :as    util]
   [boot.core          :as    b]
   [boot.task.built-in :refer [repl]]
   [boot.from.backtick :refer [template]]))

(defmacro ^:private r
  [sym]
  `(do (require '~(symbol (namespace sym))) (resolve '~sym)))

(def ^:private ws-ip    (atom nil))
(def ^:private ws-port  (atom 0))
(def ^:private out-file (atom nil))

(def ^:private deps
  (delay (remove pod/dependency-loaded? '[[com.cemerick/piggieback   "0.1.5"]
                                          [weasel                    "0.6.0-SNAPSHOT"]
                                          [org.clojure/clojurescript "0.0-2814"]])))

(defn- repl-deps []
  (let [deps       (->> (b/get-env) pod/resolve-dependencies (map :dep))
        relevant? #{'com.cemerick/piggieback 'weasel 'org.clojure/tools.nrepl
                    'org.clojure/clojurescript 'cider/cider-nrepl}]
    (concat (deref boot.repl/*default-dependencies*)
            (filter #(-> % first relevant?) deps))))

(defn- make-repl-connect-file
  [conn]
  (io/make-parents @out-file)
  (util/info "Writing %s...\n" (.getName @out-file))
  (->> (template
         ((ns adzerk.boot-cljs-repl
            (:require [weasel.repl :as repl]))
          (let [repl-conn ~conn]
            (when (and repl-conn (not (repl/alive?)))
              (repl/connect ~conn)))))
       (map pr-str) (interpose "\n") (apply str) (spit @out-file)))

(defn- weasel-connection
  [ip port ups-libs ups-foreign-libs]
  (apply (r weasel.repl.websocket/repl-env)
         :port port
         :ups-libs ups-libs
         :ups-foreign-libs ups-foreign-libs
         (when ip [:ip ip])))

(defn- weasel-port
  []
  (->> @@(r weasel.repl.server/state) :server meta :local-port))

(defn- weasel-stop
  []
  (when-let [stop (:server @@(r weasel.repl.server/state))]
    (util/info "<< stopping repl websocket server >>\n")
    (stop)))

(defn get-upstream-deps []
  "The way Clojurescript handles this does not work when
   using classloaders in the fancy ways we do."
  (->> (pod/classloader-resources "deps.cljs")
       (keep second)
       (mapcat identity)
       (map (comp read-string slurp))
       (apply merge-with concat)))

(defn repl-env
  "Start the Weasel server without attaching a REPL client immediately. This will
  return a repl-env suitable for use with cemerik.piggieback/cljs-repl.

  Keyword Options:
    :ip     str   The IP address the websocket server will listen on.
    :port   int   The port the websocket server will listen on."
  [& {i :ip p :port}]
  (let [i        (or i @ws-ip)
        p        (or p @ws-port)
        clih     (if (and i (not= i "0.0.0.0")) i "localhost")
        ups-deps (get-upstream-deps)
        repl-env (weasel-connection i p (:libs ups-deps) (:foreign-libs ups-deps))
        port     (weasel-port)
        conn     (format "ws://%s:%d" clih port)]
    (make-repl-connect-file conn)
    repl-env))

(defn start-repl
  "Start the Weasel server and attach REPL client to running browser environment.

  Keyword Options:
    :ip     str   The IP address the websocket server will listen on.
    :port   int   The port the websocket server will listen on."
  [& {i :ip p :port}]
  (let [i    (or i @ws-ip)
        p    (or p @ws-port)
        clih (if (and i (not= i "0.0.0.0")) i "localhost")]
    ((r cemerick.piggieback/cljs-repl) :repl-env (repl-env :ip i :port p))
    (let [port (weasel-port)
          conn (format "ws://%s:%d" clih port)]
      (make-repl-connect-file conn)
      nil)))

(defn- add-init!
  [in-file out-file]
  (let [ns 'adzerk.boot-cljs-repl
        spec (-> in-file slurp read-string)]
    (when (not= :nodejs (-> spec :compiler-options :target))
      (util/info "Adding :require %s to %s...\n" ns (.getName in-file))
      (io/make-parents out-file)
      (-> spec
          (update-in [:require] conj ns)
          pr-str
          ((partial spit out-file))))))

(b/deftask cljs-repl
  "Start a ClojureScript REPL server.

  The default configuration starts a websocket server on a random available
  port on localhost."

  [i ip ADDR   str "The IP address for the server to listen on."
   p port PORT int "The port the websocket server listens on."]

  (let [src (b/temp-dir!)
        tmp (b/temp-dir!)]
    (b/cleanup (weasel-stop))
    (when ip (reset! ws-ip ip))
    (when port (reset! ws-port port))
    (b/set-env! :source-paths #(conj % (.getPath src))
                :dependencies #(into % (vec (seq @deps))))
    (reset! out-file (io/file src "adzerk" "boot_cljs_repl.cljs"))
    (make-repl-connect-file nil)
    (util/dbug "Loaded REPL dependencies: %s\n" (pr-str (repl-deps)))
    (comp
      (repl :server     true
            :middleware ['cemerick.piggieback/wrap-cljs-repl])
      (b/with-pre-wrap fileset
        (doseq [f (->> fileset b/input-files (b/by-ext [".cljs.edn"]))]
          (let [path     (b/tmppath f)
                in-file  (b/tmpfile f)
                out-file (io/file tmp path)]
            (io/make-parents out-file)
            (add-init! in-file out-file)))
        (-> fileset (b/add-resource tmp) b/commit!)))))
