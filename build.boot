(set-env!
  :src-paths    #{"src"}
  :dependencies '[[org.clojure/clojure       "1.6.0"      :scope "provided"]
                  [boot/core                 "2.0.0-pre9" :scope "provided"]
                  [tailrecursion/boot-useful "0.1.3"      :scope "test"]])

(require '[tailrecursion.boot-useful :refer :all])

(def +version+ "0.1.5")

(useful! +version+)

(task-options!
  pom  [:project     'adzerk/boot-cljs-repl
        :version     +version+
        :description "Boot task to compile ClojureScript applications."
        :url         "https://github.com/adzerk/boot-cljs-repl"
        :scm         {:url "https://github.com/adzerk/boot-cljs-repl"}
        :license     {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}])
