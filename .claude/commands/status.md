---
description: Read-only — show where every feature under .specs/ stands. See .claude/commands/status.md.
argument-hint: "[feature-id]"
---
# /status

**Phase:** meta — read-only
**Owning agent:** none (pure reporting)

## Purpose
Show the user where every active feature stands. No writes, no side effects.

## Inputs
- Optional `<feature-id>`; without it, summarise all features under `.specs/`.

## Reads
- `.specs/*/01-spec.md`, `02-spec-review.md`, `03-design.md`, `04-tasks.md`, `.tdd-state.json`, `07-validation-report.md`
- `build/harness-summary.json` if present

## Writes
Nothing.

## Process
One row per feature:
- `feature_id`
- `phase` — derived from which artifacts exist + their verdicts (specify → spec-review → plan → build → validate → review → done)
- `acs_total`, `acs_with_tests`
- `tasks_done / tasks_total`
- `last_validate_verdict` + timestamp
- `active_task` from `.tdd-state.json` and its current phase

Then one sentence: "Recommended next action: …".

## Refuse if
Never. Missing data shows `—`.

## Done when
The table is printed with a recommended next command.
