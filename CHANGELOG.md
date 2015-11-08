## Unreleased

- Revisited dependency handling, user is not forced to add direct dependencies
to REPL libraries
- Filter ids `by-path` instead of `by-name` for consistency with other tasks
- Add `nrepl-opts` option
- Only modify `.cljs.edn` when it has changed

## 0.2.0 (26.9.2015)

For full change log check https://github.com/adzerk-oss/boot-cljs-repl/compare/0.1.9...0.2.0

- Use Boot 2.0.0 API
- Add `ws-host` and `secure` options
- Add `ids` option to specify to what builds specified by `.cljs.edn` files
cljs-repl should be injected into
- Ignore nodejs builds
