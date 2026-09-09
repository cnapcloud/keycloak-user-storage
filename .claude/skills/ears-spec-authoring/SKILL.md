---
name: ears-spec-authoring
description: Write acceptance criteria in EARS-lite — five clause shapes, atomic, testable, stably numbered AC-NNN. Use when drafting or reviewing 01-spec.md.
---
# EARS-lite spec authoring

Acceptance criteria are short, atomic, and testable. One condition → one outcome → one test.

## The five shapes
```
Ubiquitous:   The system shall <action>.
Event-driven: When <trigger>, the system shall <action>.
State-driven: While <state>, the system shall <action>.
Optional:     Where <feature>, the system shall <action>.
Unwanted:     If <unwanted condition>, then the system shall <mitigation>.
```

## AC id convention
- `AC-NNN`, zero-padded 3 digits, monotonically increasing within a feature.
- **Never reused** — a removed AC leaves a tombstone line.
- Tests reference it via `@Tag("AC-007")` and/or `@DisplayName("T-003: … AC-007 …")`. `.claude/scripts/traceability.sh` consumes both.

## Worked examples (this project)
- `AC-001: When a POST /user request has a username that already exists, then the system shall respond 409 and leave the store unchanged.`
- `AC-002: When PATCH /user/{id}/attributes contains a key with a null value, the system shall remove that key from the user's attributes.`
- `AC-003: While a user has no attributes, the system shall still return an empty JSON object for "attributes", never null.`

## Anti-patterns the author refuses
- "The system shall be fast." → not testable. Give a number (`p95 ≤ 200 ms at N RPS`) or drop it.
- "Probably we want…" → never invent. File a `Q-NNN`.
- One AC bundling several conditions → split into atomic ACs.
- Implementation leakage ("shall call `UserRepositoryImpl.buildPredicates`") → restate as observable behaviour.

## What does NOT belong in a spec
Implementation choices (class, library, column) → `03-design.md`. Coverage thresholds / gate definitions → the harness. Time estimates → `04-tasks.md` sizing only.
