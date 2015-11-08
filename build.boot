(set-env!
  :source-paths #{"src"}
  :dependencies '[[boot/core        "2.2.0"  :scope "provided"]
                  [adzerk/bootlaces "0.1.11" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.3.0-SNAPSHOT")

(bootlaces! +version+)

(task-options!
  pom  {:project     'adzerk/boot-cljs-repl
        :version     +version+
        :description "Boot task to provide a ClojureScript REPL."
        :url         "https://github.com/adzerk/boot-cljs-repl"
        :scm         {:url "https://github.com/adzerk/boot-cljs-repl"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})
