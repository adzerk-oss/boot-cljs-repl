(ns adzerk.boot-cljs-repl
  {:boot/export-tasks true}
  (:require [boot.core          :as    b]
            [boot.from.backtick :refer [template]]
            [boot.pod           :as    pod]
            [boot.task.built-in :refer [repl]]
            [boot.util          :as    util]
            [clojure.java.io    :as    io]
            [clojure.string     :as    str]))

(defmacro ^:private r
  [sym]
  `(do (require '~(symbol (namespace sym))) (resolve '~sym)))

(def ^:private ws-settings (atom {}))
(def ^:private out-file (atom nil))

(def ^:private deps
  (delay (remove pod/dependency-loaded? '[[com.cemerick/piggieback "0.2.1"]
                                          [org.clojure/tools.nrepl "0.2.10"]
                                          [weasel                  "0.7.0"]])))

(def min-deps
  {'org.clojure/clojurescript "0.0-3308"
   'org.clojure/tools.nrepl   "0.2.10"
   'org.clojure/tools.reader  "0.10.0-alpha1"})

(defn version->vec [v]
  (mapv #(Integer/parseInt %) (str/split v #"\.")))

(defn version-compare [v1 v2]
  (compare (version->vec v1) (version->vec v2)))

(defn- warn-deps-versions
  "Warn user if version of dependencies are too low

  Will not check for Clojure, as boot-cljs presence is assumed"
  []
  (let [deps     (map :dep (pod/resolve-dependencies (b/get-env)))
        find-dep (fn [dep] (first (filter #(-> % first #{dep}) deps)))]
    (doseq [[name version] min-deps
            :let [dep (find-dep name)]]
      (when (neg? (compare (second dep) version))
        (util/warn "WARNING: %s version %s is older than required %s\n"
          name (second dep) version)))))

(defn- repl-deps []
  (let [deps       (->> (b/get-env) pod/resolve-dependencies (map :dep))
        relevant? #{'com.cemerick/piggieback 'weasel 'org.clojure/tools.nrepl
                    'org.clojure/clojurescript 'cider/cider-nrepl}]
    (concat (deref boot.repl/*default-dependencies*)
            (filter #(-> % first relevant?) deps))))

(defn- weasel-port
  []
  (->> @@(r weasel.repl.server/state) :server meta :local-port))

(defn- make-repl-connect-file
  [conn]
  (io/make-parents @out-file)
  (when conn (util/info "Connection is %s\n" conn))
  (util/info "Writing %s...\n" (.getName @out-file))
  (->> (template
         ((ns adzerk.boot-cljs-repl
            (:require [weasel.repl :as repl]))
          (let [repl-conn ~conn]
            (when (and repl-conn (not (repl/alive?)))
              (repl/connect ~conn)))))
       (map pr-str) (interpose "\n") (apply str) (spit @out-file)))

(defn- write-repl-connect-file
  [clih secure?]
  (let [port (weasel-port)
        proto (if secure? "wss" "ws")
        conn (format "%s://%s:%d" proto clih port)]
    (make-repl-connect-file conn)))

(defn- weasel-connection
  [ip port ups-libs ups-foreign-libs pre-connect]
  (apply (r weasel.repl.websocket/repl-env)
         :port port
         :ups-libs ups-libs
         :ups-foreign-libs ups-foreign-libs
         (concat
           (when ip [:ip ip])
           (when pre-connect [:pre-connect pre-connect]))))

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
    :ip      str   The IP address the websocket server will listen on.
    :port    int   The port the websocket server will listen on.
    :ws-host str   Websocket host to connect to.
    :secure  bool  Flag to indicate whether to use a secure websocket."
  [& {i :ip p :port secure :secure ws-host :ws-host}]
  (let [i        (or i (:ws-ip @ws-settings))
        p        (or p (:ws-port @ws-settings))
        ws-host  (or ws-host (:ws-host @ws-settings))
        secure   (or secure (:secure @ws-settings))
        clih     (or ws-host (if (and i (not= i "0.0.0.0")) i "localhost"))
        ups-deps (get-upstream-deps)
        repl-env (weasel-connection i p
                   (:libs ups-deps) (:foreign-libs ups-deps)
                   #(write-repl-connect-file clih secure))]
    repl-env))

(defn start-repl
  "Start the Weasel server and attach REPL client to running browser environment.

  Keyword Options:
    :ip     str   The IP address the websocket server will listen on.
    :port   int   The port the websocket server will listen on.
    :ws-host str   Websocket host to connect to.
    :secure  bool  Flag to indicate whether to use a secure websocket."
  [& {i :ip p :port secure :secure ws-host :ws-host}]
  (let [i    (or i (:ws-ip @ws-settings))
        p    (or p (:ws-port @ws-settings))
        ws-host (or ws-host (:ws-host @ws-settings))
        secure (or secure (:secure @ws-settings))]
    ((r cemerick.piggieback/cljs-repl) (repl-env :ip i :port p :ws-host ws-host :secure secure))))

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

(defn relevant-cljs-edn [fileset ids]
  (let [relevant  (map #(str % ".cljs.edn") ids)
        f         (if ids
                    #(b/by-name relevant %)
                    #(b/by-ext [".cljs.edn"] %))]
    (-> fileset b/input-files f)))

(b/deftask cljs-repl
  "Start a ClojureScript REPL server.

  The default configuration starts a websocket server on a random available
  port on localhost."

  [b ids BUILD_IDS  #{str} "Only inject reloading into these builds (= .cljs.edn files)"
   i ip ADDR        str "The IP address for the server to listen on."
   p port PORT      int "The port the websocket server listens on."
   w ws-host WSADDR str "The (optional) websocket host address to pass to clients."
   s secure         bool "Flag to indicate whether the client should connect via wss. Defaults to false."]
  (let [src (b/tmp-dir!)
        tmp (b/tmp-dir!)]
    (b/set-env! :source-paths #(conj % (.getPath src))
                :dependencies #(into % (vec (seq @deps))))
    (warn-deps-versions)
    (b/cleanup (weasel-stop))
    (when ip (swap! ws-settings assoc :ws-ip ip))
    (when port (swap! ws-settings assoc :ws-port port))
    (when ws-host (swap! ws-settings assoc :ws-host ws-host))
    (when secure (swap! ws-settings assoc :secure secure))
    (reset! out-file (io/file src "adzerk" "boot_cljs_repl.cljs"))
    (make-repl-connect-file nil)
    (util/dbug "Loaded REPL dependencies: %s\n" (pr-str (repl-deps)))
    (comp
      (repl :server     true
            :middleware ['cemerick.piggieback/wrap-cljs-repl])
      (b/with-pre-wrap fileset
        (doseq [f (relevant-cljs-edn fileset ids)]
          (let [path     (b/tmp-path f)
                in-file  (b/tmp-file f)
                out-file (io/file tmp path)]
            (io/make-parents out-file)
            (add-init! in-file out-file)))
        (-> fileset (b/add-resource tmp) b/commit!)))))
