# Spec Format — EARS-lite

Acceptance criteria use **EARS-lite**: five clause shapes, no formal grammar, no tooling lock-in.
Short, testable, traceable.

## Why
- **Atomic** — one condition, one outcome → one test.
- **Testable** — each shape maps to a Given/When/Then assertion.
- **Traceable** — each AC has a stable id referenced from tasks, tests, code, and the validation matrix.

## The five shapes
```
Ubiquitous:   The system shall <action>.
Event-driven: When <trigger>, the system shall <action>.
State-driven: While <state>, the system shall <action>.
Optional:     Where <feature>, the system shall <action>.
Unwanted:     If <unwanted condition>, then the system shall <mitigation>.
```

## AC id convention
- `AC-NNN` — zero-padded 3 digits, monotonically increasing within a feature.
- **Never reused.** A removed AC leaves a tombstone line.
- Tests reference it via `@Tag("AC-007")` and/or `@DisplayName("T-003: … AC-007 …")`.
  `.claude/scripts/traceability.sh` consumes both.

## Worked example (this project)
```markdown
## Acceptance Criteria
- AC-001: When POST /user has a username that already exists, then the system shall respond 409
          with {"error": "..."} and leave the store unchanged.
- AC-002: When PATCH /user/{id}/attributes contains a key mapped to null, the system shall remove
          that key from the user's attributes.
- AC-003: While a user has no attributes, the system shall return "attributes" as an empty object,
          never null.
- AC-004: If /user/{id} is unknown on DELETE, then the system shall respond 404.
```

## What does NOT belong in a spec
- Implementation choices (class, library, column) → `03-design.md`.
- Coverage thresholds, gate definitions → the harness.
- Time estimates → `04-tasks.md` sizing only.

## Anti-patterns the spec author refuses
- "The system shall be fast." → give a measurable NFR or drop it.
- "Probably we want…" → never invent; becomes a `Q-NNN`.
- One AC bundling several conditions → split.
- "the system shall call `UserServiceImpl.createUser`" → restate as observable behaviour.
