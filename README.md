# boot-cljs-repl

```clojure
[adzerk/boot-cljs-repl "0.1.9"] ;; latest release
```

or snapshot
[](dependency)
```clojure
[adzerk/boot-cljs-repl "0.2.0"] ;; latest release
```
[](/dependency)

[Boot] task providing a ClojureScript browser REPL via [Weasel] and [Piggieback].

## Clojure Version 1.7.0

The current version of `boot-cljs-repl` requires Boot to use Clojure 1.7.0. You
can control the version of Clojure used by boot as described here:
https://github.com/boot-clj/boot/wiki/Setting-Clojure-version


So if you use a boot.properties file it should look something like this:
```
BOOT_CLOJURE_VERSION=1.7.0
BOOT_VERSION=2.1.2
```

## Usage

Add `boot-cljs-repl` to your `build.boot` dependencies and `require` the
namespace:

```clj
(set-env! :dependencies '[[adzerk/boot-cljs-repl "X.Y.Z" :scope "test"]])
(require '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])
```

It's also a good idea to explicitly depend on the specific version of Clojure
and ClojureScript needed for your application, such as in the following example:

```clj
(set-env!
  :dependencies '[
    [adzerk/boot-cljs      "0.0-3308-0"      :scope "test"]
    [adzerk/boot-cljs-repl "0.1.10-SNAPSHOT" :scope "test"]
    [adzerk/boot-reload    "0.3.1"           :scope "test"]
    [pandeiro/boot-http    "0.6.3-SNAPSHOT"  :scope "test"]
    [org.clojure/clojure "1.7.0"]
    [org.clojure/clojurescript "0.0-3308"]])
```

A typical `boot.build` file for ClojureScript development:

```clj
(set-env!
  :src-paths    #{"src"}
  :dependencies '[[adzerk/boot-cljs      "0.0-X-Y" :scope "test"]
                  [adzerk/boot-cljs-repl "X.Y.Z"   :scope "test"]
                  [org.clojure/clojure "X.Y.Z"]
                  [org.clojure/clojurescript "X.Y.Z"]])

(require
  '[adzerk.boot-cljs      :refer :all]
  '[adzerk.boot-cljs-repl :refer :all])
```

## Help

You can see the task options available on the command line:

```bash
$ boot cljs-repl -h
```

or in the REPL:

```clj
boot.user=> (doc cljs-repl)
```

## Build

Start a build pipeline with file-watcher, ClojureScript REPL server, and
compile ClojureScript with source maps, unified HTML loading, and no
optimizations:

```bash
# note: cljs-repl task must precede cljs task
$ boot watch cljs-repl cljs -sO none
```

## REPL

To start evaluating forms in the browser you must first connect to the running
Clojure nREPL server (started by the `cljs-repl` task above) and create a new,
browser-connected CLJS REPL.

#### Terminal

```bash
$ boot repl -c
```

```clj
boot.user=> (start-repl)
```

#### Emacs Cider

```
M-x cider-connect
```

```clj
boot.user=> (start-repl)
```

####  Vim Fireplace

```clj
:Piggieback (adzerk.boot-cljs-repl/repl-env)
```

## License

Copyright © 2014-15 Adzerk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://github.com/tailrecursion/boot
[2]: http://clojars.org/adzerk/boot-cljs-repl/latest-version.svg?cache=4
[3]: http://clojars.org/adzerk/boot-cljs-repl
[Boot]: https://github.com/boot-clj/boot
[Cider]: https://github.com/clojure-emacs/cider
[Weasel]: https://github.com/tomjakubowski/weasel
[piggieback]: https://github.com/cemerick/piggieback
