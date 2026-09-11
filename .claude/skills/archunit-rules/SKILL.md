---
name: archunit-rules
description: The architecture rules the layer conventions imply, as ArchUnit test specs — Controller≠Repository, layered access, naming. Use in /plan (add rules per feature) and /validate.
---
# ArchUnit rules

Turns `spring-layer-conventions` from prose into executable rules.

## Rule set
| # | Rule | Rationale |
|---|---|---|
| A1 | Classes in `..controller..` must not depend on `..repository..` | Controller calls Service only |
| A2 | Classes in `..repository..` must not depend on `..controller..` or `..service..` | one-way layering |
| A3 | Only `..service..` may depend on `..repository..` | layered access |
| A4 | `@RestController` classes have simple name ending `Controller` | naming |
| A5 | `..service..` interfaces end `Service`; impls end `ServiceImpl` | naming |
| A6 | No class uses `java.lang.reflect` for field writes | attributes-map rule, no reflection |
| A7 | No cycles between top-level packages | keep the module acyclic |
| A8 | No `System.out` / `System.err` — use `@Slf4j` | logging convention |

## Brownfield
Freeze pre-existing violations with `FreezingArchRule.freeze(rule)` so onboarding doesn't block; new violations fail. Record the frozen count in `.specs/_baseline.json`.

## Per-feature additions
When `03-design.md` introduces a new package boundary (e.g. an `internal` sub-package), `/plan` adds the matching rule to this list and to `04-tasks.md` as a cross-cutting task.
