# boot-cljs-repl

[![Clojars Project][2]][3]

Boot task providing a ClojureScript browser REPL via [weasel].

## Usage

Add `boot-cljs-repl` to your `build.boot` dependencies and `require` the
namespace:

```clj
(set-env! :dependencies '[[adzerk/boot-cljs-repl "X.Y.Z" :scope "test"]])
(require '[adzerk.boot-cljs-repl :refer :all])
```

You can see the options available on the command line:

```bash
boot cljs-repl -h
```

or in the REPL:

```clj
boot.user=> (doc cljs-repl)
```

### Setup

A typical `boot.build` file for ClojureScript development:

```clj
(set-env!
  :src-paths    #{"src"}
  :dependencies '[[adzerk/boot-cljs      "0.0-2371-13" :scope "test"]
                  [adzerk/boot-cljs-repl "0.1.4"       :scope "test"]])

(require
  '[adzerk.boot-cljs      :refer :all]
  '[adzerk.boot-cljs-repl :refer :all])
```

When compiling with optimization level `none` and you're not using the `-u`
option with the `cljs` task you must add a script tag to the page HTML to
connect the client to the REPL server:

```html
<!--
  Note: This is only needed when optimization level is :none and the -u
  (--unified) option is not specified for the cljs task.
  -->
<script type="text/javascript">goog.require('adzerk.boot_cljs_repl');</script>
```

### Build

Start a build pipeline with file-watcher, start ClojureScript REPL, and compile
ClojureScript with source maps, unified HTML loading, and no optimizations:

```bash
# note: cljs-repl task must precede cljs task
boot watch cljs-repl cljs -usO none
```

### Connect REPL Client

To start the CLJS REPL you must connect to the running REPL server. In the
terminal you can do:

```bash
boot repl -c
```

but you can also connect from other REPL clients, like via [cider] in Emacs,
for example:

```
M-x cider RET RET RET
```

> If cider can't figure out which port the server is listening on you may
> inspect the `.nrepl-port` file was created in the project directory by
> the REPL server.

### Start CLJS REPL

In the connected REPL client do:

```clj
boot.user=> (start-repl)
```

Load your page in a browser. Boom. REPL.

## License

Copyright © 2014 Adzerk

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://github.com/tailrecursion/boot
[2]: http://clojars.org/adzerk/boot-cljs-repl/latest-version.svg?cache=3
[3]: http://clojars.org/adzerk/boot-cljs-repl
[cider]: https://github.com/clojure-emacs/cider
[weasel]: https://github.com/tomjakubowski/weasel
