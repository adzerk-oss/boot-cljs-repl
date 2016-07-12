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
(def ^:private cljs-repl-opts (atom {}))

(def ^:private deps
  '[[com.cemerick/piggieback "0.2.1" :scope "test"]
    [weasel                  "0.7.0" :scope "test"]
    [org.clojure/tools.nrepl "0.2.12" :scope "test"]])

(defn- assert-deps
  "Advices user to add direct deps to requires deps if they
  are not available."
  []
  (let [current  (->> (b/get-env :dependencies)
                     (map first)
                     set)
        missing  (->> deps
                      (remove (comp current first)))]
    (if (seq missing)
      (util/warn (str "You are missing necessary dependencies for boot-cljs-repl.\n"
                      "Please add the following dependencies to your project:\n"
                      (str/join "\n" missing) "\n")))))

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
        p        (or p (:ws-port @ws-settings) 0)
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
    :secure  bool  Flag to indicate whether to use a secure websocket.
    :repl-opts map options passed to the cljs-repl"
  [& {i :ip p :port secure :secure ws-host :ws-host repl-opts :repl-opts}]
  (let [i    (or i (:ws-ip @ws-settings))
        p    (or p (:ws-port @ws-settings) 0)
        ws-host (or ws-host (:ws-host @ws-settings))
        secure (or secure (:secure @ws-settings))]
    (apply (r cemerick.piggieback/cljs-repl)
           (repl-env :ip i :port p :ws-host ws-host :secure secure)
           (mapcat identity (merge @cljs-repl-opts repl-opts)))))

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

(defn- relevant-cljs-edn [prev fileset ids]
  (let [relevant  (map #(str % ".cljs.edn") ids)
        f         (if ids
                    #(b/by-path relevant %)
                    #(b/by-ext [".cljs.edn"] %))]
    (-> (b/fileset-diff prev fileset) b/input-files f)))

(b/deftask cljs-repl-env
  "Setup a weasel / piggieback env.

  The default configuration starts a websocket server on a random available
  port on localhost."

  [b ids BUILD_IDS         #{str} "Only inject reloading into these builds (= .cljs.edn files)"
   i ip ADDR               str "The IP address for the server to listen on."
   p port PORT             int "The port the websocket server listens on."
   w ws-host WSADDR        str "The (optional) websocket host address to pass to clients."
   s secure                bool "Flag to indicate whether the client should connect via wss. Defaults to false."
   d dir DIR               str "The output-dir for repl-compiled Javascript. Defaults to \"target/main.out\""
   r repl-opts REPL_OPTS   edn "Options passed to the cljs-repl"]
  (let [src (b/tmp-dir!)
        tmp (b/tmp-dir!)
        prev (atom nil)]
    (b/set-env! :source-paths #(conj % (.getPath src)))
    (assert-deps)
    (b/cleanup (weasel-stop))
    (when ip (swap! ws-settings assoc :ws-ip ip))
    (when port (swap! ws-settings assoc :ws-port port))
    (when ws-host (swap! ws-settings assoc :ws-host ws-host))
    (when secure (swap! ws-settings assoc :secure secure))
    (swap! cljs-repl-opts assoc :output-dir (or dir "target/main.out"))
    (when repl-opts (swap! cljs-repl-opts merge repl-opts))
    (reset! out-file (io/file src "adzerk" "boot_cljs_repl.cljs"))
    (make-repl-connect-file nil)
    (b/with-pre-wrap fileset
      (doseq [f (relevant-cljs-edn @prev fileset ids)]
        (let [path     (b/tmp-path f)
              in-file  (b/tmp-file f)
              out-file (io/file tmp path)]
          (io/make-parents out-file)
          (add-init! in-file out-file)))
      (reset! prev fileset)
      (-> fileset (b/add-resource tmp) b/commit!))))

(b/deftask cljs-repl
  "Start a ClojureScript REPL server.

  The default configuration starts a websocket server on a random available
  port on localhost."

  [b ids BUILD_IDS         #{str} "Only inject reloading into these builds (= .cljs.edn files)"
   i ip ADDR               str "The IP address for the server to listen on."
   n nrepl-opts NREPL_OPTS edn "Options passed to the `repl` task."
   p port PORT             int "The port the websocket server listens on."
   w ws-host WSADDR        str "The (optional) websocket host address to pass to clients."
   s secure                bool "Flag to indicate whether the client should connect via wss. Defaults to false."]
  (let [piggie-repl (partial repl :server true
                             :middleware ['cemerick.piggieback/wrap-cljs-repl])]
    (comp
      (if nrepl-opts
        (apply piggie-repl (mapcat identity nrepl-opts))
        (piggie-repl))
      (apply cljs-repl-env (mapcat identity (dissoc *opts* :nrepl-opts))))))
