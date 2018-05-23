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
  '[[cider/piggieback "0.3.5" :scope "test"]
    [weasel           "0.7.0" :scope "test"]
    [nrepl            "0.3.1" :scope "test"]])

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
              (repl/connect ~conn :print #{:repl :console})))))
       (map pr-str)
       (interpose "\n")
       (apply str)
       (spit @out-file)))

(defn- write-repl-connect-file
  [clih secure?]
  (let [port (weasel-port)
        proto (if secure? "wss" "ws")
        conn (format "%s://%s:%d" proto clih port)]
    (make-repl-connect-file conn)))

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
  [& {:keys [ip port secure ws-host] :as opts}]
  (let [ip       (or ip (:ws-ip @ws-settings))
        port     (or port (:ws-port @ws-settings) 0)
        ws-host  (or ws-host (:ws-host @ws-settings))
        secure   (or secure (:secure @ws-settings))
        listen-host (or ws-host (if (and ip (not= ip "0.0.0.0")) ip "localhost"))
        ups-deps (get-upstream-deps)]
    (apply (r weasel.repl.websocket/repl-env)
           (mapcat identity
                   (merge {;; :ws-host ws-host
                           :port port
                           :ups-libs (:libs ups-deps)
                           :ups-foreign-libs (:foreign-libs ups-deps)
                           :pre-connect #(write-repl-connect-file listen-host secure)}
                          (if ip {:ip ip})
                          (dissoc opts :ip :secure :ws-host :port))))))

(defn start-repl
  "Start the Weasel server and attach REPL client to running browser environment.

  Keyword Options:
  :ip     str   The IP address the websocket server will listen on.
  :port   int   The port the websocket server will listen on.
  :ws-host str   Websocket host to connect to.
  :secure  bool  Flag to indicate whether to use a secure websocket.
  :cljs-repl-opts edn Repl options passed to the Piggieback client."
  [& {:keys [ip port secure ws-host cljs-repl-opts] :as opts}]
  (apply (r cider.piggieback/cljs-repl) (apply repl-env (mapcat identity (dissoc opts :cljs-repl-opts)))
         (mapcat identity cljs-repl-opts)))

(defn- add-init!
  [tmp cljs-edn-path spec]
  (let [ns 'adzerk.boot-cljs-repl
        out-file (io/file tmp cljs-edn-path)]
    (io/make-parents out-file)
    (when (not= :nodejs (-> spec :compiler-options :target))
      (util/info "Adding :require %s to %s...\n" ns cljs-edn-path)
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
   s secure                bool "Flag to indicate whether the client should connect via wss. Defaults to false."]
  (let [src (b/tmp-dir!)
        tmp (b/tmp-dir!)
        prev (atom nil)]
    ;; The client file is rewriten when calling repl-env, this is why this has to be
    ;; on :source-paths, to enable recompilation when the file is written.
    ;; FIXME: Is there alternative way to trigger recompilation without messing with :source-paths?
    (b/set-env! :source-paths #(conj % (.getPath src)))
    (assert-deps)
    (b/cleanup (weasel-stop))
    ;; FIXME: This is a mess. Options from task are stored in a
    ;; atom for latter use in repl-env or cljs-repl. Is there any
    ;; alternative? Rename the atom at least.
    (when ip (swap! ws-settings assoc :ws-ip ip))
    (when port (swap! ws-settings assoc :ws-port port))
    (when ws-host (swap! ws-settings assoc :ws-host ws-host))
    (when secure (swap! ws-settings assoc :secure secure))
    (reset! out-file (io/file src "adzerk" "boot_cljs_repl.cljs"))
    (make-repl-connect-file nil)
    (fn [next-handler]
      (fn [fileset]
        (let [cljs-edns (relevant-cljs-edn @prev fileset ids)]
          ;; Write cljs-repl client files per cljs.edn
          (doseq [f cljs-edns
                  :let [spec (-> f b/tmp-file slurp read-string)
                        path (b/tmp-path f)]]
            (add-init! tmp path spec))
          (reset! prev fileset)
          (-> fileset
              ;; Also on :source-paths, but sometimes the files is not found the first time
              ;; probably due to set-env! side-effects.
              (b/add-source src)
              (b/add-resource tmp)
              b/commit!
              next-handler))))))

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
  (comp
    ;; FIXME: concat :middleware?
    (apply repl (mapcat identity (merge nrepl-opts
                                        {:server true
                                         :middleware ['cemerick.piggieback/wrap-cljs-repl]})))
    (apply cljs-repl-env (mapcat identity (dissoc *opts* :nrepl-opts)))))
