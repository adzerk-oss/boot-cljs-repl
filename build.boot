(set-env!
  :resource-paths #{"src"}
  :dependencies '[[cider/piggieback "0.3.9" :scope "test"]
                  [weasel           "0.7.0" :scope "test"]
                  [nrepl            "0.4.5" :scope "test"]])

(def +version+ "0.4.0")

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
