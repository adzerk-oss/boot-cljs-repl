## Unreleased

- Add `:cljs-repl-opts` to `start-repl`, allows passing [CLJS Repl options](https://clojurescript.org/reference/repl-options)

## 0.3.3 (18.7.2016)

- Default to both REPL and console printing ([#43](https://github.com/adzerk-oss/boot-cljs-repl/pull/43))

## 0.3.2 (22.6.2016)

- Add `repl-env` task which should be useful with `cider-jack-in` ([#42](https://github.com/adzerk-oss/boot-cljs-repl/pull/42))

[Changelog](https://github.com/adzerk-oss/boot-cljs-repl/compare/0.3.0...0.3.2)

(0.3.1 release is broken)

## 0.3.0 (8.11.2015)

- **BREAKING**: Revisited dependency handling, user is now forced to add direct dependencies
to REPL libraries
- Filter ids `by-path` instead of `by-name` for consistency with other tasks
- Add `nrepl-opts` option
- Only modify `.cljs.edn` when it has changed

[Changelog](https://github.com/adzerk-oss/boot-cljs-repl/compare/0.2.0...0.3.0)

## 0.2.0 (26.9.2015)

For full change log check https://github.com/adzerk-oss/boot-cljs-repl/compare/0.1.9...0.2.0

- Use Boot 2.0.0 API
- Add `ws-host` and `secure` options
- Add `ids` option to specify to what builds specified by `.cljs.edn` files
cljs-repl should be injected into
- Ignore nodejs builds
