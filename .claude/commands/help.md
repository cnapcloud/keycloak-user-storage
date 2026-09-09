---
description: Read-only — print the command catalog and phase order. See .claude/commands/help.md.
argument-hint: "[command-name]"
---
# /help

**Phase:** meta — read-only
**Owning agent:** none

## Purpose
Print the command catalog and the recommended phase order. Optionally explain a single command in depth.

## Inputs
- Optional `<command-name>` (no leading slash).

## Reads
- `.claude/commands/`
- `.claude/commands/<command-name>.md` if a name was supplied
- `.claude/docs/methodology.md`

## Process
No argument → print this table and the natural-language alias list:

| Command | Phase | Produces |
|---|---|---|
| `/onboard` | 0 | `.specs/_onboarding.md`, `_stack.json`, `_baseline.json` |
| `/spec` | 1 | `01-spec.md` |
| `/spec-review` | 2 | `02-spec-review.md` |
| `/plan` | 3 | `03-design.md`, `04-tasks.md`, `adr/`, `.tdd-state.json` |
| `/build T-NNN` | 4 | code + tests, `05-implementation-log.md` |
| `/validate` | 5 | `07-validation-report.md`, `07a-traceability.md` |
| `/review` | 6 | `08-code-review.md` |
| `/status` | meta | pipeline table (read-only) |
| `/help` | meta | this catalog |

Order: `/onboard → /spec → /spec-review → /plan → /build T-001 … → /validate → /review → commit`.

With an argument → print that command's Purpose + Inputs, then its owning agent's Process / Refuse if / Done when / Hard rules / Handoff from `.claude/agents/<name>.md` (the thin command file points there; `/status` and `/help` have no agent).

## Refuse if
Never.

## Done when
Help text is rendered.
