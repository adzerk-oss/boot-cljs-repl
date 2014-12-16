# boot-cljs-repl

[![Clojars Project][2]][3]

[Boot] task providing a ClojureScript browser REPL via [Weasel] and [Piggieback].

## Usage

Add `boot-cljs-repl` to your `build.boot` dependencies and `require` the
namespace:

```clj
(set-env! :dependencies '[[adzerk/boot-cljs-repl "X.Y.Z" :scope "test"]])
(require '[adzerk.boot-cljs-repl :refer :all])
```

You can see the options available on the command line:

```bash
$ boot cljs-repl -h
```

or in the REPL:

```clj
boot.user=> (doc cljs-repl)
```

## Setup

A typical `boot.build` file for ClojureScript development:

```clj
(set-env!
  :src-paths    #{"src"}
  :dependencies '[[adzerk/boot-cljs      "0.0-X-Y" :scope "test"]
                  [adzerk/boot-cljs-repl "X.Y.Z"   :scope "test"]])

(require
  '[adzerk.boot-cljs      :refer :all]
  '[adzerk.boot-cljs-repl :refer :all])
```

When compiling with optimization level `none` and you're not using the `-u`
option with the `cljs` task you must add a script tag to the page HTML to
connect the client to the REPL server:

```html
<!-- Note: This is only needed when optimization level is :none
     and the -u (--unified) option is not specified for the cljs task. -->
<script type="text/javascript">goog.require('adzerk.boot_cljs_repl');</script>
```

## Build

Start a build pipeline with file-watcher, ClojureScript REPL server, and
compile ClojureScript with source maps, unified HTML loading, and no
optimizations:

```bash
# note: cljs-repl task must precede cljs task
$ boot watch cljs-repl cljs -usO none
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
M-x cider RET RET RET
```

```clj
boot.user=> (start-repl)
```

####  Vim Fireplace

```clj
:Piggieback (repl-env)
```

## License

Copyright © 2014 Adzerk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://github.com/tailrecursion/boot
[2]: http://clojars.org/adzerk/boot-cljs-repl/latest-version.svg?cache=4
[3]: http://clojars.org/adzerk/boot-cljs-repl
[Boot]: https://github.com/boot-clj/boot
[Cider]: https://github.com/clojure-emacs/cider
[Weasel]: https://github.com/tomjakubowski/weasel
[piggieback]: https://github.com/cemerick/piggieback
