# boot-cljs-repl

[![Clojars Project][2]][3]

Boot task providing a ClojureScript REPL via [weasel].

## Usage

Add `boot-cljs-repl` to your `build.boot` dependencies and `require` the
namespace:

```clj
(set-env! :dependencies '[[tailrecursion/boot-cljs-repl "X.Y.Z" :scope "test"]])
(require '[tailrecursion.boot-cljs-repl :refer :all])
```

You can see the options available on the command line:

```bash
boot cljs-repl -h
```

or in the REPL:

```clj
boot.user=> (doc cljs-repl)
```

A typical `boot.build` file for ClojureScript development:

```clj
(set-env!
  :src-paths    #{"src"}
  :rsc-paths    #{"html"}
  :dependencies '[[tailrecursion/boot-cljs      "0.0-2371-11" :scope "test"]
                  [tailrecursion/boot-cljs-repl "0.1.0"       :scope "test"]])

(require
  '[tailrecursion.boot-cljs      :refer :all]
  '[tailrecursion.boot-cljs-repl :refer :all])
```

Start a build pipeline with file-watcher, start ClojureScript REPL, and compile
with no optimizations:

```bash
boot watch cljs-repl cljs -O none
```

> **Note:** when compiling with optimization level `none`, you must add this
> script tag to the page HTML to connect the client to the REPL server:
> 
> ```html
> <script type="text/javascript">goog.require('tailrecursion.boot_cljs_repl');</script>
> ```

## License

Copyright © 2014 Micha Niskin and Alan Dipert

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://github.com/tailrecursion/boot
[2]: http://clojars.org/tailrecursion/boot-cljs-repl/latest-version.svg?cache=2
[3]: http://clojars.org/tailrecursion/boot-cljs-repl
[cider]: https://github.com/clojure-emacs/cider
