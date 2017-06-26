(set-env!
  :resource-paths #{"src"}
  :dependencies '[[com.cemerick/piggieback "0.2.2" :scope "test"]
                  [weasel                  "0.7.0" :scope "test"]
                  [org.clojure/tools.nrepl "0.2.13" :scope "test"]])

(def +version+ "0.4.0-SNAPSHOT")

(task-options!
  pom  {:project     'adzerk/boot-cljs-repl
        :version     +version+
        :description "Boot task to provide a ClojureScript REPL."
        :url         "https://github.com/adzerk/boot-cljs-repl"
        :scm         {:url "https://github.com/adzerk/boot-cljs-repl"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build []
  (comp
   (pom)
   (jar)
   (install)))

(deftask dev []
  (comp
    (watch)
    (build)
    (repl :server true)))

(deftask deploy []
  (comp
   (build)
   (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))
