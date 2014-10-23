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

When compiling with optimization level `none` you must add a script tag to the
page HTML to connect the client to the REPL server:

```html
<!-- note: this is only needed when optimization level is :none -->
<script type="text/javascript">goog.require('adzerk.boot_cljs_repl');</script>
```

### Build

Start a build pipeline with file-watcher, start ClojureScript REPL, and compile
with no optimizations:

```bash
# note: cljs-repl task must precede cljs task
boot watch cljs-repl cljs -O none
```

### Start REPL

Connect to the REPL server you just started and do:

```clj
boot.user=> (start-repl)
```

Load your page in a browser. Boom. REPL.

## License

Copyright © 2014 Micha Niskin and Alan Dipert

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://github.com/tailrecursion/boot
[2]: http://clojars.org/adzerk/boot-cljs-repl/latest-version.svg?cache=3
[3]: http://clojars.org/adzerk/boot-cljs-repl
[cider]: https://github.com/clojure-emacs/cider
[weasel]: https://github.com/tomjakubowski/weasel
