---
name: clarity-over-cleverness
description: The simplify pass — untangle clever code into obvious code without changing behaviour. Use in /build phase 4 and /review section "clarity".
---
# Clarity over cleverness

Run after refactor, before "done". Behaviour must not change; the suite stays green.

## Moves
- **Untangle nested ternaries** → `if`/`else` or early return.
- **Early return** over deep nesting / accumulating flags.
- **Kill dead options** — unused parameters, config branches nothing hits, speculative interfaces.
- **Name domain concepts** — use the `01-spec.md` glossary. `u-XXXXXXXX` string building → a named helper `newUserId()`.
- **Inline once-used helpers** whose name adds nothing.
- **Extract repeated literals** — any string/number appearing 2+ times in a file → `private static final`.
- **One responsibility per method** — a method doing "validate + persist + serialise" splits.

## Don't
- Don't add abstraction for a single caller.
- Don't change public signatures or behaviour (that's refactor / a new task).
- Don't rewrite legacy code outside `files_in_scope` just because it's clever.

## In /review
Flag clever hunks as `should-fix` or `nit` with a concrete rewrite. Do not auto-apply — that's the implementer's simplify pass.
