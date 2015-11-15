# boot-cljs-repl

[](dependency)
```clojure
[adzerk/boot-cljs-repl "0.3.0"] ;; latest release
```
[](/dependency)

[Boot] task providing a ClojureScript browser REPL via [Weasel] and [Piggieback].

This task **requires Clojure 1.7** to learn how to pin the Clojure version in a Boot project
head to the [Boot wiki](https://github.com/boot-clj/boot/wiki/Setting-Clojure-version).

**NOTE**: Version 0.3.0 changed how the REPL dependencies are handled. For now user is required to add dependencies to necessary libraries to one's project. Cljs-repl task will print the required dependecies when run. <sup><a href="#direct_dependencies">[1]</a></sup>

## Usage

Add `boot-cljs-repl` to your `build.boot` dependencies and `require` the
namespace:

```clj
(require '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])
```

`cljs-repl` is the task to be used in the task pipeline whereas `start-repl`
is how you connect to the ClojureScript REPL once you're in a Clojure REPL.

> It's also a good idea to explicitly depend on the specific version of Clojure
> and ClojureScript needed for your application.

### Build

Start a build pipeline with file-watcher, ClojureScript REPL server, and
compile ClojureScript with no optimizations:

```bash
# note: cljs-repl task must precede cljs task
$ boot watch cljs-repl
```
or
```clojure
(deftask dev []
  (comp (watch)
        (cljs-repl) ; order is important!!
        (cljs)))
```

For optional configuration see `boot cljs -h` and `boot cljs-repl -h`.

**Important:** The `cljs-repl` task injects things into your build so
if you run the compiler before the `cljs-repl` task is being run your
REPL will not work.

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

## Help

You can see the task options available on the command line:

```bash
$ boot cljs-repl -h
```

or in the REPL:

```clj
boot.user=> (doc cljs-repl)
```

## Notes

### Direct dependencies

For now user needs to add direct dependencies to REPL libs ([piggieback], [weasel])
to one's project. This is because Boot tasks shouldn't modify the classpath of the
application by adding new dependencies, either by calling `set-env!` or though
transitive dependencies. The preferred way for Boot tasks to use additional libraries
is through [pods](https://github.com/boot-clj/boot/wiki/Pods) but because the REPL
is currently run on the main pod (where your application code runs), it's not
currently possible to use this.

## License

Copyright © 2014-15 Adzerk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[2]: http://clojars.org/adzerk/boot-cljs-repl/latest-version.svg?cache=4
[3]: http://clojars.org/adzerk/boot-cljs-repl
[Boot]: https://github.com/boot-clj/boot
[Cider]: https://github.com/clojure-emacs/cider
[Weasel]: https://github.com/tomjakubowski/weasel
[piggieback]: https://github.com/cemerick/piggieback
